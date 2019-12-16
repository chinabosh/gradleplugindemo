package com.bosh.plugin

import com.bosh.task.TestTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class TestPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.create("hello", TestTask.class, new Action<TestTask>() {
            @Override
            void execute(TestTask t) {
                t.setMsg("external msg")
                t.sayHello()
            }
        })

        def android = project.extensions.getByType(com.android.build.gradle.AppExtension)
        println('test plugin')
    }
}
