package com.bosh.plugin


import com.bosh.task.TestTask
import org.gradle.api.*

class ComponentLibraryPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println('test plugin')
        project.rootProject
        project.android {
            defaultConfig {
                javaCompileOptions {
                    annotationProcessorOptions {
                        arguments = [AROUTER_MODULE_NAME: project.getName()]
                    }
                }
            }

            lintOptions{
                abortOnError true
                allWaringingsAsError true
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
            checkAndroid(project)
            if (project.hasProperty('android')) {

                project.android.compileOptions.sourceCompatibility = JavaVersion.VERSION_1_8
                project.android.compileOptions.targetCompatibility = JavaVersion.VERSION_1_8

                checkResourcePrefix(project)

            }
        }

    }

    private static void checkResourcePrefix(Project project) {
        String prefix = project.android.resourcePrefix
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

    private static void checkAndroid(Project project) {
        if (!project.android) {
            throw new GradleException('must apply from \"com.android.application\" or \"com.android.library\" first')
        }
    }
}
