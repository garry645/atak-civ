package com.atakmap.gradle.takdev

import org.gradle.api.DefaultTask
import com.atakmap.gradle.takdev.BuildConfigLint
import org.gradle.api.tasks.TaskAction

abstract class TakDevLint extends DefaultTask {
    @TaskAction
    def takdevLint() {
        BuildConfigLint bcl = new BuildConfigLint(project)
        // Checks for application projects
        if (project.plugins.hasPlugin("com.android.application")) {
            bcl.minifyEnabled()
            bcl.repackageCheck()
            bcl.storeArchiveDisabled()
        }
        bcl.legacyPackagingOptionsEnabled()
    }
}
