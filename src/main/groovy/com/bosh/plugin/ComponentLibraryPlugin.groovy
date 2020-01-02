package com.bosh.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.bosh.task.TestTask
import com.bosh.transform.RestrictToTransform
import com.bosh.utils.Logger
import org.gradle.api.*

class ComponentLibraryPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        Logger.i("plugin demo begin!")
        project.rootProject
        project.android {
            Logger.i("Arouter module name setting")
            defaultConfig {
                javaCompileOptions {
                    annotationProcessorOptions {
                        arguments = [AROUTER_MODULE_NAME: project.getName()]
                    }
                }
            }

            lintOptions{

                //should deal all warnings
                abortOnError true
                warningsAsErrors true

                // improve the priority of RestrictedApi
                enable "RestrictedApi"
                fatal "RestrictedApi"

                ignore "InvalidPackage"
            }
        }
        project.afterEvaluate {
            Logger.i('create task: hello')
            project.tasks.create("hello", TestTask.class, new Action<TestTask>() {
                @Override
                void execute(TestTask t) {
                    t.setMsg("external msg")
                    t.sayHello()
                }
            })
            checkAndroid(project)
            if (project.hasProperty('android')) {

                project.android.compileOptions.sourceCompatibility = JavaVersion.VERSION_1_8
                project.android.compileOptions.targetCompatibility = JavaVersion.VERSION_1_8

                checkResourcePrefix(project)

            }
        }
    }

    /**
     * require that all library module resource must have prefix!
     * @param project
     */
    private static void checkResourcePrefix(Project project) {
        AppExtension android = getAndroid(project)
        String prefix = android.resourcePrefix
        if (!prefix?.trim()) {
            throw new GradleException('module must have resourcePrefix at \'build.gradle\',like:\n' +
                    'android {\n' +
                    '    defaultConfig {\n' +
                    '        ...\n' +
                    '        resourcePrefix \'[module_name]_\'\n' +
                    '    }\n' +
                    '}\n')
        }
    }

    private static AppExtension getAndroid(Project project) {
        return project.getExtensions().findByType(AppExtension.class)
    }

    private static LibraryExtension getLibrary(Project project) {
        return project.getExtensions().findByType(LibraryExtension.class)
    }

    private static void checkAndroid(Project project) {
        if (!project.android) {
            throw new GradleException('must apply from \"com.android.application\" or \"com.android.library\" first')
        }
    }

    private static void addRestrict(Project project) {
        project.extensions.findByType(BaseExtension.class).registerTransform(new RestrictToTransform())
    }
}
