package com.bosh.transform

import com.alibaba.android.arouter.register.utils.ScanSetting
import com.bosh.utils.Logger
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.NotFoundException
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.EnumMemberValue
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class GenerateRestrictTo {
    static ClassPool classPool = ClassPool.getDefault()


    static void generate(File dir, Project project) {
        classPool.appendClassPath(dir.absolutePath)
        classPool.appendClassPath(project.android.bootClasspath[0].toString())//android.jar
        classPool.importPackage("androidx.annotation.RestrictTo")
        classPool.importPackage("android.os.Bundle")
        if (dir.isDirectory()) {
            dir.eachFileRecurse { file ->
                doGenerate(file, dir.absolutePath)
            }
        }
    }

    static void doGenerate(File file, String path) {
        def name = file.absolutePath
        if (name.endsWith(".class")) {
//            Logger.w("class name:" + name)
            try {
                int index = name.indexOf("com/")
                if (index > -1) {
                    name = name.substring(index, name.length() - 6)
                    name = name.replaceAll("/", ".")
                    CtClass cc = classPool.getCtClass(name)
                    def checkResult = !check(cc)
                    if (checkResult) {
                        return
                    }
                    generate(cc, path)
                    Logger.w("generate absolute path:" + file.absolutePath)
                }
            } catch (NotFoundException e) {
//                        Logger.e("RestrictTo not found:" + e.getMessage())
            }
        } else {
//            Logger.w("not endsWith .class:" + name)
        }
    }

    static boolean check(CtClass ctClass) {
        def res = false
        String name = ctClass.name
        if (name.contains("bosh") && !name.contains("R\$") && !name.contains("R2\$")) {
            String className = name.substring(name.lastIndexOf(".") + 1)
            if ("R".equals(className) || "BuildConfig".equals(className) || className.contains("Manifest")) {
                res = false
            } else {
                if (ctClass.annotations == null) {
                    res = true
                } else {
                    res = true
                    ctClass.annotations.each { Object object ->
                        if (object instanceof java.lang.annotation.Annotation) {
                            def typeName = object.annotationType().typeName
                            if (typeName.contains("com.china.bosh.mylibrary.annotation")
                                    || typeName.contains("androidx.annotation.RestrictTo")) {
                                Logger.w("typeName:" + typeName)
                                res = false
                            }
                        }
                    }
                }
            }

        }
        return res
    }

    static generate(CtClass cc, String path) {
        def name = cc.name
        Logger.w("generate RestrictTo to before:" + name)
        cc = modifyClass(cc)
        cc.writeFile(path)
        cc.detach()
    }

    static byte[] generate(CtClass cc) {
        def bytes
        def name = cc.name
        Logger.w("generate RestrictTo to before:" + name)
        if (cc.frozen) {
            cc.defrost()
        }
        cc = modifyClass(cc)

        bytes = cc.toBytecode()
        cc.detach()
        return bytes
    }

    private static CtClass modifyClass(CtClass cc) {
        if (cc.frozen) {
            cc.defrost()
        }
        ClassFile classFile = cc.classFile
        ConstPool constPool = classFile.constPool

        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
        Annotation annotation = new Annotation("androidx.annotation.RestrictTo", constPool)
        EnumMemberValue enumMemberValue = new EnumMemberValue(constPool)
        enumMemberValue.setType("androidx.annotation.RestrictTo\$Scope")
        enumMemberValue.setValue('LIBRARY')
        annotation.addMemberValue("value", enumMemberValue)
        annotationsAttribute.addAnnotation(annotation)
        classFile.addAttribute(annotationsAttribute)
        return cc
    }

    static File insertRestrictCodeIntoJarFile(File jarFile, Project project) {
        classPool.appendClassPath(jarFile.absolutePath)
        classPool.appendClassPath(project.android.bootClasspath[0].toString())//android.jar
        classPool.importPackage("androidx.annotation.RestrictTo")
        classPool.importPackage("java.lang.annotation")
        if (jarFile) {
            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")
            if (optJar.exists())
                optJar.delete()
            def file = new JarFile(jarFile)
            Enumeration enumeration = file.entries()
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = file.getInputStream(jarEntry)
                jarOutputStream.putNextEntry(zipEntry)
                Logger.w("entryName:" + entryName)
                if (entryName.contains("bosh")) {
                    def cc = classPool.makeClass(inputStream)
                    if (check(cc)) {

                        Logger.w('Insert init code to class >> ' + entryName)
                        def bytes = generate(cc)
                        jarOutputStream.write(bytes)
                    } else {
                        jarOutputStream.write(IOUtils.toByteArray(inputStream))
                    }
                }
                inputStream.close()
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            file.close()

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
        }
        return jarFile
    }


}
