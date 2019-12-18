package com.bosh.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.CompileOptions
import com.android.build.gradle.internal.dsl.AnnotationProcessorOptions
import com.android.build.gradle.internal.dsl.DefaultConfig
import com.android.build.gradle.internal.dsl.JavaCompileOptions
import com.bosh.task.TestTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.CompilerArgumentProvider
import org.gradle.process.CommandLineArgumentProvider

class TestPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println('test plugin')
        project.android {
            defaultConfig {
                javaCompileOptions {
                    annotationProcessorOptions {
                        arguments = [AROUTER_MODULE_NAME: project.getName()]
                    }
                }
            }
        }

        project.beforeEvaluate {
            if (project.hasProperty('android')) {
                println('set Arouter gradle setting')
                LibraryExtension android = project.android
                android.defaultConfig(new Action<DefaultConfig>() {
                    @Override
                    void execute(DefaultConfig defaultConfig) {
                        defaultConfig.javaCompileOptions(new Action<JavaCompileOptions>() {
                            @Override
                            void execute(JavaCompileOptions javaCompileOptions) {
                                javaCompileOptions.annotationProcessorOptions(new Action<AnnotationProcessorOptions>() {
                                    @Override
                                    void execute(AnnotationProcessorOptions annotationProcessorOptions) {
                                        println('excute add module name arguments')
                                        annotationProcessorOptions.arguments(['AROUTER_MODULE_NAME': project.getName()])
                                        String name = annotationProcessorOptions.arguments.get('AROUTER_MODULE_NAME')
                                        println('module name:' + name)
                                    }
                                })
                            }
                        })
                    }
                })
            } else {

            }
        }

        project.afterEvaluate {
            println('create task: hello')
            project.tasks.create("hello", TestTask.class, new Action<TestTask>() {
                @Override
                void execute(TestTask t) {
                    t.setMsg("external msg")
                    t.sayHello()
                }
            })
            if (!project.android) {
                throw new GradleException('must apply from \"com.android.application\" or \"com.android.library\" first')
            }
            if (project.hasProperty('android')) {

                project.android.compileOptions.sourceCompatibility = JavaVersion.VERSION_1_8
                project.android.compileOptions.targetCompatibility = JavaVersion.VERSION_1_8

                if (!project.android.resourcePrefix?.trim()) {
                    throw new GradleException('module must have resourcePrefix,try type below in build.gradle' +
                            'android {\n' +
                            '    defaultConfig {\n' +
                            '        resourcePrefix \'[module_name]_\'\n' +
                            '    }\n' +
                            '}\n')
                } else {
                    println('module ' + project.getName() + ' has resourcePrefix!')
                }

            }
        }

    }
}
