package com.atakmap.gradle.takdev

import org.gradle.api.GradleException
import org.gradle.api.Project

class BuildConfigLint {

    Project project

    BuildConfigLint(Project _project) {
        project = _project
    }

    def minifyEnabled() {
        if (project.android.buildTypes.release.minifyEnabled != true) {
            throw new GradleException("Obfuscation disabled. Please set minifyEnabled to true.")
        }
    }

    def storeArchiveDisabled() {
        if (project.android.bundle.storeArchive.enable != false) {
            throw new GradleException("android.bundle.storeArchive.enable is enabled. Please set storeArchive to false for proper release signing.")
        }
    }

    def legacyPackagingOptionsEnabled() {
        if (project.android.packagingOptions.jniLibs.useLegacyPackaging != true) {
            throw new GradleException("android.packagingOptions.jniLibs.useLegacyPackaging is disabled. Please set useLegacyPackaging for proper plugin library loading.")
        }
    }



    def repackageCheck() {
        FileReader fileIn
        BufferedReader br
        try {
            File pGuard = project.android.buildTypes.release.proguardFiles[0]
            fileIn = new FileReader(pGuard)
            br = new BufferedReader(fileIn)
            String tmp = br.readLine()
            Boolean exit = false
            while (tmp != null && !exit) {
                if (tmp.contains('-repackageclasses')) {
                    exit = true
                    if (tmp.contains('atakplugin.PluginTemplate')) {
                        String plgName = new TakDevPlugin().getValueFromPropertiesFile(new File("${project.rootDir}/app/src/main/AndroidManifest.xml"), 'package')
                        if (!plgName.contains("plugintemplate.plugin")) {
                            throw new GradleException("Update -repackageclasses in the proguard configuration to a value unique for your plugin.")
                        }
                    }
                }
                tmp = br.readLine()
            }
        } catch (Exception e) {
            if (e instanceof GradleException) {
                throw e
            }
            else {
                println e
            }
        } finally {
            fileIn.close()
        }
    }
}
