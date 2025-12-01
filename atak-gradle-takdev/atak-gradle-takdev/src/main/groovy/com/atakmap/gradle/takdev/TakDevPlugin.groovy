package com.atakmap.gradle.takdev

import groovy.io.FileType
import groovy.xml.QName

import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Task
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import org.apache.commons.io.IOUtils
import org.gradle.api.invocation.Gradle

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.regex.Matcher
import java.util.regex.Pattern

class TakDevPlugin implements Plugin<Project> {

    enum VariantType {
        UNKNOWN,
        APPLICATION,
        LIBRARY
    }

    class PathTuple {
        File apiJar
        File keystore
        File mapping
        File coreRules

        @Override
        String toString() {
            return "apiJar => ${apiJar.absolutePath}, keystore => ${keystore.absolutePath}, mapping => ${mapping.absolutePath}, coreRules => ${coreRules.absolutePath}"
        }
    }

    boolean verbose = false

    void debugPrintln(Object msg) {
        if (verbose)
            println(msg)
    }

    def getValueFromPropertiesFile (File propFile, String key) {
        if(!propFile.isFile() || !propFile.canRead())
            return null
        def prop = new Properties()
        def reader = propFile.newReader()
        try {
            prop.load(reader)
        } finally {
            reader.close()
        }
        return prop.get(key)
    }

    def populateKeystoreConfig (Project project) {
        def props = new File("${project.rootDir}/local.properties")

        if (props.isFile()) {
            def prop = new Properties()
            def fileIn = new FileInputStream(props)

            try {
                prop.load(fileIn)
            } finally {
                fileIn.close()
            }

            if (getValueFromPropertiesFile(props, "takDebugKeyFile") == null) {
                prop.setProperty("takDebugKeyFile", "${project.buildDir}/android_keystore")
            }
            if (getValueFromPropertiesFile(props, "takDebugKeyFilePassword") == null) {
                prop.setProperty("takDebugKeyFilePassword", "tnttnt")
            }
            if (getValueFromPropertiesFile(props, "takDebugKeyAlias") == null) {
                prop.setProperty("takDebugKeyAlias", "wintec_mapping")
            }
            if (getValueFromPropertiesFile(props, "takDebugKeyPassword") == null) {
                prop.setProperty("takDebugKeyPassword", "tnttnt")
            }
            if (getValueFromPropertiesFile(props, "takReleaseKeyFile") == null) {
                prop.setProperty("takReleaseKeyFile", "${project.buildDir}/android_keystore")
            }
            if (getValueFromPropertiesFile(props, "takReleaseKeyFilePassword") == null) {
                prop.setProperty("takReleaseKeyFilePassword", "tnttnt")
            }
            if (getValueFromPropertiesFile(props, "takReleaseKeyAlias") == null) {
                prop.setProperty("takReleaseKeyAlias", "wintec_mapping")
            }
            if (getValueFromPropertiesFile(props, "takReleaseKeyPassword") == null) {
                prop.setProperty("takReleaseKeyPassword", "tnttnt")
            }

            def writer = props.newWriter()

            try {
                prop.store(writer, "")
            } finally {
                writer.close()
            }
        }
    }

    void addMetadataToAndroidManifest(Project project, String variant, Map metadata) {
        if (project.buildDir == null) {
            throw new GradleException("The build directory does not exist so the AndroidManifest.xml files cannot be updated.")
        }

        def lowerVariant = variant.toLowerCase()
        def manifestFiles = []
        project.buildDir.eachFileRecurse(FileType.FILES) { file ->
            if (file.name == 'AndroidManifest.xml') {
                if (file.toString().toLowerCase().contains(lowerVariant)) {
                    manifestFiles << file
                }
            }
        }

        if (manifestFiles.size() == 0) {
            throw new GradleException("No AndroidManifest.xml files found in ${project.buildDir} for ${variant}")
        }

        manifestFiles.each {
            boolean changed = false
            def manifest = new XmlParser().parse(it)
            def applicationElement = manifest.application[0]

            if (applicationElement == null) {
                throw new GradleException("No <application> element found in AndroidManifest.xml")
            }

            def key = new QName("http://schemas.android.com/apk/res/android", "name")
            metadata.each { name, value -> {
                Node existingElement = null

                // Find the entry if it already exists
                applicationElement.'meta-data'.each { element ->
                    if (element.attributes().get(key) == "plugin-id") {
                        existingElement = element
                    }
                }

                if (existingElement != null) {
                    debugPrintln("Updating the meta-data plugin id "+value)
                    existingElement.attributes().replace(
                        new QName("http://schemas.android.com/apk/res/android", "value"), value)
                } else {
                    debugPrintln("Inserting new meta-data plugin id "+value)
                    new Node(applicationElement, 'meta-data', [
                        "android:name": name,
                        "android:value": value
                    ])
                }
            }}

            def writer = new FileWriter(it)
            def xmlOutput = new XmlNodePrinter(new PrintWriter(writer))
            xmlOutput.setPreserveWhitespace(true)
            xmlOutput.print(manifest)
            writer.close()
        }
    }

    void apply(Project project) {
        project.ext.devkitVersion = getLocalOrProjectProperty(project, 'takrepo.devkit.version', 'devkitVersion', project.ATAK_VERSION)
        if (project.devkitVersion != '0.0.0.0' && 0 > versionComparator(project.devkitVersion, '4.2.0')) {
            throw new GradleException("Incompatible takdev version. This plugin should be major version 1 to support ${project.devkitVersion}")
        }

        verbose = getLocalOrProjectProperty(project, 'takdev.verbose', null, 'false').equals('true')

        String appVariant = VariantType.APPLICATION == getVariantType(project) ? 'true' : 'false'
        String libVariant = VariantType.LIBRARY == getVariantType(project) ? 'true' : 'false'

        project.tasks.register('takdevLint', TakDevLint)

        project.afterEvaluate {

            Map taskList = project.getAllTasks(true)
            Task[] taskArr = taskList.get(project).toArray()

            for (int i = 0; i < taskList.get(project).size(); i++) {
                if (taskArr[i].getName().contains("lint")) {
                    taskArr[i].dependsOn(project.takdevLint)
                }

                // Add a do first
                Pattern pattern = ~/process(.*)(Release|Debug)MainManifest/
                def matcher = pattern.matcher(taskArr[i].getName())
                if (matcher.find()) {
                    project.tasks.named(taskArr[i].getName()) {
                        doLast {
                            def metadata = [:]
                            if (project.ext.takdevMetadataPluginId != null && !"null".equals(project.ext.takdevMetadataPluginId)) {
                                metadata["plugin-id"] = project.ext.takdevMetadataPluginId
                            }

                            if (metadata.size() > 0) {
                                addMetadataToAndroidManifest(project, matcher.group(1)+matcher.group(2), metadata)
                            }
                        }
                    }
                }
            }
        }
        InputStream fileIn
        OutputStream fileOut
        try {
            new File("${project.rootDir}/.takdev/aars").mkdirs()
            File takDevLintAar = new File("${project.rootDir}/.takdev/aars/takdevlint.aar")
            fileIn = getClass().getResourceAsStream("/META-INF/takdevlint.aar")
            fileOut = new FileOutputStream(takDevLintAar)
            IOUtils.copy(fileIn, fileOut)
        } catch (Exception e) {
            println e
        } finally {
            if (fileIn != null) {
                fileIn.close()
            }
            if (fileOut != null) {
                fileOut.close()
            }
        }

        try {
            File takDevLintAarOld = new File("${project.rootDir}/aars/takdevlint.aar")
            takDevLintAarOld.delete();
        } catch (Exception e) { }

        project.repositories {
            flatDir {
                dirs "${project.rootDir}/.takdev/aars"
            }
        }

        // Consuming gradle project already handles 'isDevKitEnabled', 'takrepoUrl', 'takrepoUser' and 'takrepoPassword'
        project.ext.mavenOnly = getLocalOrProjectProperty(project, 'takrepo.force', 'mavenOnly', 'false').equals('true')
        project.ext.snapshot = getLocalOrProjectProperty(project, 'takrepo.snapshot', 'snapshot', 'true').equals('true')
        project.ext.requireMavenLocal = getLocalOrProjectProperty(project, 'takrepo.requireMavenLocal', 'requireMavenLocal', 'false').equals('true')
        project.ext.sdkPath = getLocalOrProjectProperty(project, 'sdk.path', 'sdkPath', "${project.rootDir}/sdk")
        project.ext.takdevProduction = getLocalOrProjectProperty(project, 'takdev.production', 'takdevProduction', 'false').equals('true')
        project.ext.takdevNoApp = getLocalOrProjectProperty(project, 'takdev.noapp', 'takdevNoApp', libVariant).equals('true')
        project.ext.takdevConTestEnable = getLocalOrProjectProperty(project, 'takdev.contest.enable', 'takdevConTestEnable', appVariant).equals('true')
        project.ext.takStaticVersion = getLocalOrProjectProperty(project, 'takStaticVersion', 'takStaticVersion', '-1')

        project.ext.takdevConTestVersion = getLocalOrProjectProperty(project, 'takdev.contest.version', 'takdevConTestVersion', project.devkitVersion)
        project.ext.takdevConTestPath = getLocalOrProjectProperty(project, 'takdev.contest.path', 'takdevConTestPath', "${project.rootDir}/espresso")

        project.ext.takdevMetadataPluginId = getLocalOrProjectProperty(project, 'takdev.metadata.pluginid', 'takdevMetadataPluginId', null)

        // Ideally, the isDevKitEnabled variable should be renamed, as it indicates whether we're using a remote repository.
        // However, there are now a number of plugins that have this variable name through deep copies of plugintemplate
        debugPrintln("isDevKitEnabled => ${project.isDevKitEnabled()}")
        debugPrintln("takrepo URL option => ${project.takrepoUrl}")
        debugPrintln("takrepo user option => ${project.takrepoUser}")
        debugPrintln("mavenOnly option => ${project.mavenOnly}")
        debugPrintln("snapshot option => ${project.snapshot}")
        debugPrintln("devkitVersion option => ${project.devkitVersion}")
        debugPrintln("sdkPath option => ${project.sdkPath}")
        debugPrintln("production option => ${project.takdevProduction}")
        debugPrintln("takStaticVersion option => ${project.takStaticVersion}")
        debugPrintln("noapp option => ${project.takdevNoApp}")
        debugPrintln("connected test enable => ${project.takdevConTestEnable}")
        debugPrintln("connected test version => ${project.takdevConTestVersion}")
        debugPrintln("connected test path => ${project.takdevConTestPath}")
        debugPrintln("plugin id => ${project.takdevMetadataPluginId}")

        if (project.mavenOnly) {
            if (project.isDevKitEnabled) {
                configureMaven(project)
            } else {
                throw new GradleException("No remote repo available to configure TAK DevKit as mavenOnly")
            }
        } else {
            def tuple = getAutoBuilder(project)
            boolean offline = null == tuple
            if (offline) {
                tuple = getOfflineDevKit(project)
            }
            if (null == tuple) {
                if (project.isDevKitEnabled) {
                    println("Configuring Maven TAK plugin build")
                    configureMaven(project)
                } else {
                    throw new GradleException("No remote repo or local files available to configure TAK DevKit")
                }
            } else {
                offline ? println("Configuring Offline TakDev plugin build") :
                        println("Configuring Autobuilder TakDev plugin build")
                configureOffline(project, tuple)
            }
        }

        // define tasks
        addTasks(project)
        addUtilities(project)

        // inject a synthetic placeholder for any non-domestic variant implied via the requested
        // build task. If the plugin explicitly specifies the same flavor via its build script, the
        // properties for the placeholder are overwritten
        def domesticFlavors = ['civ', 'gov', 'mil']
        def buildFlavors = getBuildFlavors(project)

        buildFlavors.each { buildFlavor ->
            if(project.plugins.hasPlugin('com.android.application') && buildFlavor != null && !domesticFlavors.contains(buildFlavor.uncapitalize())) {
                def syntheticFlavor = buildFlavor.uncapitalize()
                def productFlavor = project.android.productFlavors.create("${syntheticFlavor}")
                productFlavor.dimension = 'application'
                productFlavor.applicationIdSuffix = ".${syntheticFlavor}"
                productFlavor.matchingFallbacks = ['civ']
                productFlavor.manifestPlaceholders = [atakApiVersion: "com.atakmap.app@" + project.ATAK_VERSION + ".${syntheticFlavor.toUpperCase()}"]
            }
        }

    }


    static void addTasks(Project project) {
        project.ext.getTargetVersion = project.tasks.register('getTargetVersion', DefaultTask) {
            group 'metadata'
            description 'Gets this plugin\'s targeted ATAK version'
            doLast {
                println(project.devkitVersion)
            }
        }
    }

    static String getGitInfo(Project project, List arg_list) throws Exception {
        def info = ""
        new ByteArrayOutputStream().withStream { os ->
            project.exec {
                executable = 'git'
                args = arg_list
                standardOutput = os
                ignoreExitValue = true
            }
            info = os.toString().trim()
        }
        return info
    }

    static LinkedHashMap<String, Serializable> findFlavor(List<LinkedHashMap<String, Serializable>> m, String name) {
        for (LinkedHashMap<String, Serializable> i : m) {
            String n = i.get("name")
            if (n != null)
                if (n.equalsIgnoreCase(name)) return i
        }
        return null
    }

    static String getBuildFlavor(Project project) {
        def tskReqStr = project.gradle.startParameter.taskRequests.toString()

        Pattern pattern
        if( tskReqStr.contains( "assemble" ) )
            pattern = Pattern.compile("assemble(\\w+)(Release|Debug|Sdk|Odk)")
        else if( tskReqStr.contains( "install" ) )
            pattern = Pattern.compile("install(\\w+)(Release|Debug|Sdk|Odk)")
        else
            pattern = Pattern.compile("generate(\\w+)(Release|Debug|Sdk|Odk)")

        def matcher = pattern.matcher(tskReqStr)
        return matcher.find() ? matcher.group(1) : null
    }

    static List<String> getBuildFlavors(Project project) {
        def tskReqs = project.gradle.startParameter.taskRequests
        def flavorSet = [] as Set
        tskReqs.each { rqst ->
            rqst.getArgs().each { task ->
                Pattern pattern
                if( task.contains( "assemble" ) )
                    pattern = Pattern.compile("assemble(\\w+)(Release|Debug|Sdk|Odk)")
                else if( task.contains( "install" ) )
                    pattern = Pattern.compile("install(\\w+)(Release|Debug|Sdk|Odk)")
                else
                    pattern = Pattern.compile("generate(\\w+)(Release|Debug|Sdk|Odk)")

                def matcher = pattern.matcher(task)
                if (matcher.find()) {
                    flavorSet.add(matcher.group(1).trim())
                }
            }
        }
        return flavorSet as List
    }

    /**
     * Called as part of productFlavors () in order to augment the current list of flavors
     * @param project the project to use
     * @param m the current list of all flavors provided by the plugin
     * @param fallback a fallback to be used if generating a new synthetic flavor.  A plugin
     * can generate a non synthetic variant if different values are required.   It is recommended
     * that the fallback always be the least restrictive distribution such as civ.
     */
    static void generateAllFlavors(Project project, List<LinkedHashMap<String, Serializable>> m, String fallback) {

        String  tskReqStr = project.getGradle().getStartParameter().getTaskRequests().toString()

        Pattern pattern
        if( tskReqStr.contains( "assemble" ) )
            pattern = Pattern.compile("assemble(\\w+)(Release|Debug|Sdk|Odk)")
        else if( tskReqStr.contains( "install" ) )
            pattern = Pattern.compile("install(\\w+)(Release|Debug|Sdk|Odk)")
        else
            pattern = Pattern.compile("generate(\\w+)(Release|Debug|Sdk|Odk)")

        Matcher matcher = pattern.matcher(tskReqStr)

        String val
        if( matcher.find() ) {
            val = matcher.group(1)
        } else {
            return
        }



        LinkedHashMap<String, Serializable> t = findFlavor(m, val)
        if (t == null) {
            println("flavor $val not defined, synthetically generating it from: $fallback")
            LinkedHashMap<String, Serializable> civTask = findFlavor(m, fallback)
            if (civTask != null) {
                t = new LinkedHashMap<String, Serializable>(civTask)
                t.put("name", val)
                t.remove("default")
                m.add(t)
            }
        }

    }

    static void addUtilities(project) {

        // Attempt to get a suitable version name for the plugin based on
        // either a git or svn repository
        project.ext.getVersionName = {
            def version = "1"
            try {
                version = getGitInfo(project, ['rev-parse', '--short=8', 'HEAD'])
                println("versionName[git]: $version")
            } catch (Exception ignored) {
                println("error occured, using version of $version")
            }
            return version
        }

        // Attempt to get a suitable version code for the plugin based on
        // either a git or svn repository
        project.ext.getVersionCode = {

            def vc = project.ext.takStaticVersion
            if (vc != null) {
                try {
                    def vci = vc.toInteger()
                    if (vci >= 0) {
                        println("Using static version from properties: " + vci)
                        return vci
                    }
                } catch (Exception ignored) {
                    throw new GradleException('takStaticVersion is not an integer - stopping build')
                }
            }

            def revision = 1
            try {
                String outputAsString = getGitInfo(project, ['show', '-s', '--format=%ct'])
                revision = outputAsString.toInteger()
                println("version[git]: $revision")
            } catch (Exception ignored) {
                println("error occured, using revision of $revision")
            }
            return revision
        }


        // Attempt to get a suitable version code for the plugin based on
        // either a git or svn repository
        project.ext.generateAllFlavors = {
             m, fallback ->
            try {
                generateAllFlavors(project, m, fallback)
            } catch (Exception e) {
                println("error occurred during generateAllFlavors", e)
            }
        }
    }

    static String getLocalOrProjectProperty(Project project, String localKey, String projectKey, String defval) {
        if (new File('local.properties').exists()) {
            def localProperties = new Properties()
            localProperties.load(project.rootProject.file('local.properties').newDataInputStream())
            def value = localProperties.get(localKey)
            if ((null != value)) {
                return value
            }
        }
        return project.properties[localKey] ?: String.valueOf(project.properties.get(projectKey, defval))
    }

    static String resolvePathFromSet(String[] filePaths, String fileName) {
        for (String path : filePaths) {
            File candidate = new File(path, fileName)
            if (candidate.exists()) {
                return path
            }
        }
        return null
    }

    PathTuple getAutoBuilder(Project project) {
        def tuple = new PathTuple()
        tuple.apiJar = new File("${project.rootDir}/../../ATAK/app/build/libs/main.jar")
        tuple.keystore = new File("${project.rootDir}/../../android_keystore")
        tuple.mapping = new File("${project.rootDir}/../../ATAK/app/build/outputs/mapping/release/mapping.txt")
        tuple.coreRules = new File("${project.rootDir}/../../ATAK/app/proguard-release-keep.txt")

        // mapping does not have to exist for local debug builds
        return tuple.apiJar.exists() && tuple.keystore.exists() ? tuple : null
    }

    PathTuple getOfflineDevKit(Project project) {
        String[] apiPaths = [
                "${project.rootDir}/../..",
                project.sdkPath
        ]
        def offlinePath = resolvePathFromSet(apiPaths, 'main.jar')
        def tuple = new PathTuple()
        tuple.apiJar = new File("${offlinePath}/main.jar")
        tuple.keystore = new File("${offlinePath}/android_keystore")
        tuple.mapping = new File("${offlinePath}/mapping.txt")
        tuple.coreRules = new File("${offlinePath}/proguard-release-keep.txt")

        // mapping does not have to exist and a blank will be created with a warning.
        return tuple.apiJar.exists() ? tuple : null
    }

    void configureOffline(Project project, PathTuple tuple) {

        debugPrintln(tuple)

        // Connected test support
        if (project.takdevConTestEnable && !project.takdevConTestPath.isEmpty()) {
            def contestFile = "${project.takdevConTestPath}/testSetup.gradle"
            if (new File(contestFile).exists()) {
                project.apply(from: contestFile)
                debugPrintln("Resolved and applied connected test artifacts from local path, ${project.takdevConTestPath}")
            } else {
                println("Warning: local test files not found. Skipping connected tests.")
            }
        }

        def variants = getVariantSet(project)
        variants.all { variant ->

            Dependency dep = project.dependencies.create(project.files(tuple.apiJar.absolutePath))
            project.dependencies.add("${variant.name}CompileOnly", dep)
            project.dependencies.add("test${variant.name.capitalize()}Implementation", dep)
            if ('debug' == variant.buildType.name) {
                project.dependencies.add("${variant.name}AndroidTestCompileClasspath", dep)
            }

            def devFlavor = getDesiredTpcFlavorName(variant, true)
            def devType = variant.buildType.name
            def mappingName = "proguard-${devFlavor}-${devType}-mapping.txt"
            def mappingFqn = tuple.mapping.absolutePath
            def coreRulesName = "proguard-release-keep.txt"
            def coreRulesFqn = tuple.coreRules.absolutePath

            def preBuildProvider = project.tasks.named("pre${variant.name.capitalize()}Build")
            preBuildProvider.configure({
                doFirst {
                    if (new File(mappingFqn).exists()) {
                        project.copy {
                            from mappingFqn
                            into project.buildDir
                            rename {
                                return mappingName
                            }
                        }
                    } else {
                        def mappingFile = project.file(mappingFqn)
                        if (!mappingFile.getParentFile().exists())
                            mappingFile.getParentFile().mkdirs()
                        project.file(mappingFqn).text = ""
                        println("${variant.name} => WARNING: no mapping file could be established, obfuscating just the plugin to work with the development core")
                    }
                    System.setProperty("atak.proguard.mapping", mappingFqn)


                    if (versionComparator(project.ATAK_VERSION, '5.4.0') >= 0) {
                        // notionally add the core rules here
                        print("augment the proguard file with the atak core rules")
                        project.android.buildTypes.release.getProguardFiles().add(project.rootProject.file("${coreRulesFqn}"))
                    } 
                    
                }
            })

            def signingClosure = {
                doFirst {
                    // Keystore
                    def storeName = 'android_keystore'
                    project.copy {
                        from tuple.keystore.absolutePath
                        into project.buildDir
                        rename {
                            return storeName
                        }
                    }
                }
            }

            // inject keystore before validate signing
            ["validateSigning${variant.name.capitalize()}",
             "validateSigning${variant.name.capitalize()}AndroidTest"].each {
                try {
                    def signingProvider = project.tasks.named(it)
                    signingProvider.configure(signingClosure)
                } catch (UnknownTaskException ute) {
                    debugPrintln("Unknown Task, skippiing ${it}.")
                }
            }
        }
    }

    void configureMaven(Project project) {

        // add the maven repo as a dependency
        MavenArtifactRepository takrepo = project.repositories.maven({
            url = project.takrepoUrl
            name = 'takrepo'
            credentials {
                username project.takrepoUser
                password project.takrepoPassword
            }
        })
        if(project.requireMavenLocal)
            project.repositories.add(project.repositories.mavenLocal())

        int[] versionTokens = splitVersionString(project.devkitVersion)
        String lowerBound = "${versionTokens.join('.')}-SNAPSHOT"
        versionTokens[versionTokens.length - 1] += 1 // increment for upper
        String upperBound = "${versionTokens.join('.')}-SNAPSHOT"
        String mavenVersion = project.snapshot ? "[${lowerBound}, ${upperBound})" : "(${lowerBound}, ${upperBound})"

        def usesCoreMappingRules =  (versionComparator(project.ATAK_VERSION, '5.4.0') >= 0)
        debugPrintln("Using Core Mapping Rules ${usesCoreMappingRules} for version ${project.devkitVersion}")

        populateKeystoreConfig(project)

        // Connected test support
        if (project.takdevConTestEnable && !project.takdevConTestVersion.isEmpty()) {
            def contestCoord = [group: 'com.atakmap.gradle', name: 'atak-connected-test', version: project.takdevConTestVersion]
            def contestDep = project.dependencies.create(contestCoord)
            def detachedConfig = project.configurations.detachedConfiguration(contestDep)
            try {
                def contestFiles = detachedConfig.resolve()
                if (contestFiles.isEmpty()) {
                    println("Warning: Skipping connected tests, no files from maven tuple ${contestCoord}")
                } else {
                    // this path has to be outside of buildDir, because a 'clean' task would other remove the artifacts
                    def contestPath = "${project.rootDir}/espresso"
                    project.copy {
                        from project.zipTree(contestFiles[0])
                        into contestPath
                    }
                    project.apply(from: "${contestPath}/testSetup.gradle")
                    debugPrintln("Resolved and applied connected test artifacts from maven tuple ${contestCoord}")
                }
            } catch (ResolveException re) {
                println("Warning: Skipping connected tests, could not resolve maven tuple ${contestCoord}")
            }
        }

        // always use `civ` as matching fallback
        project.android.productFlavors.all { productFlavor ->
            if(productFlavor.name != 'civ')
                productFlavor.matchingFallbacks += 'civ'
        }
        def variants = getVariantSet(project)
        variants.all { variant ->
            // arbitrary, variant specific, configuration names
            def apkZipConfigName = "${variant.name}ApkZip"
            def mappingConfigName = "${variant.name}Mapping"
            def keystoreConfigName = "${variant.name}Keystore"
            def coreRulesConfigName = "${variant.name}CoreRules"

            def devFlavor, devType, mavenGroupApp, mavenGroupCommon, mavenGroupTyped, mavenCoord, mavenCoordJavadoc
            def fallback = false
            def javadocAvailable = false;

            while (true) {
                // accommodate uses where variants may not be defined;  default to 'civ'
                devFlavor = getDesiredTpcFlavorName(variant, fallback)
                devType = variant.buildType.name
                if (!variant.buildType.matchingFallbacks.isEmpty() &&
                        !(project.takdevProduction && ('release' == variant.buildType.name))) {
                    devType = variant.buildType.matchingFallbacks[0]
                }
                // if we still have a buildType of 'debug', use the 'sdk' buildType
                if ('debug' == devType) {
                    devType = 'sdk'
                }
                // if we still have a non-production buildType of 'release', use the 'odk' buildType
                if (!project.takdevProduction && 'release' == devType) {
                    devType = 'odk'
                }

                mavenGroupApp = 'com.atakmap.app'
                mavenGroupCommon = "${mavenGroupApp}.${devFlavor}.common"
                mavenGroupTyped = "${mavenGroupApp}.${devFlavor}.${devType}"

                // The corner stone, the API coordinates
                mavenCoord = [group: mavenGroupCommon, name: 'api', version: mavenVersion]
                debugPrintln("${variant.name} => Using repository API, ${mavenCoord}")

                mavenCoordJavadoc = [group: mavenGroupCommon, name: 'javadoc', version: mavenVersion]
                debugPrintln("${variant.name} => Using repository Javadoc, ${mavenCoordJavadoc}")

                // Test javadoc artifact resolution.
                if (!tryResolve(project, takrepo, mavenCoordJavadoc)) {
                    println("Warning: Failed to resolve remote javadoc for ${variant.name}.")
                    javadocAvailable = false;
                } else {
                    javadocAvailable = true
                }

                //Test artifact resolution; terminate on success.
                if (!tryResolve(project, takrepo, mavenCoord)) {
                    if (fallback == false) {
                        println("Warning: Failed to resolve remote for ${variant.name}. Trying fallback.")
                        fallback = true
                    } else {
                        println("Warning: Failed to resolve remote for ${variant.name}. Skipping ${variant.name}.")
                        return
                    }
                } else {
                    break
                }
            }

            // add the Maven API coordinate as a dependency
            project.dependencies.add("${variant.name}CompileOnly", mavenCoord)
            project.dependencies.add("test${variant.name.capitalize()}Implementation", mavenCoord)
            if ('debug' == variant.buildType.name) {
                project.dependencies.add("${variant.name}AndroidTestCompileClasspath", mavenCoord)
            }

            // add the Maven Javadoc coordinate as a dependency (if available)
            if(javadocAvailable) {
                project.dependencies.add("${variant.name}CompileOnly", mavenCoordJavadoc)
                project.dependencies.add("test${variant.name.capitalize()}Implementation", mavenCoordJavadoc)
                if ('debug' == variant.buildType.name) {
                    project.dependencies.add("${variant.name}AndroidTestCompileClasspath", mavenCoordJavadoc)
                }
            }

            // other artifacts are strongly typed per variant
            mavenCoord.group = mavenGroupTyped

            // add the APK zip as a dependency
            def apkZipConfiguration
            if (!project.takdevNoApp) {
                mavenCoord.name = 'apk'
                apkZipConfiguration = project.configurations.register(apkZipConfigName)
                project.dependencies.add(apkZipConfigName, mavenCoord)
                debugPrintln("${variant.name} => Using repository APK, ${mavenCoord}")
                if ('civ' != devFlavor) {
                    def mavenCivCoord = [group: "${mavenGroupApp}.civ.${devType}", name: mavenCoord.name, version: mavenCoord.version]
                    project.dependencies.add(apkZipConfigName, mavenCivCoord)
                    debugPrintln("${variant.name} => Adding repository APK, ${mavenCivCoord}")
                }
            }

            // add the mapping as a dependency
            mavenCoord.name = 'mapping'
            def mappingConfiguration = project.configurations.register(mappingConfigName)
            project.dependencies.add(mappingConfigName, mavenCoord)
            debugPrintln("${variant.name} => Using repository mapping, ${mavenCoord}")

            NamedDomainObjectProvider<Configuration> coreRulesConfiguration = null
            if(usesCoreMappingRules) {
                mavenCoord.name = 'coreRules'
                def coreRulesMavenCoord = ('civ' != devFlavor) ?
                    [group: "${mavenGroupApp}.civ.${devType}", name: mavenCoord.name, version: mavenCoord.version] :
                    mavenCoord
                coreRulesConfiguration = project.configurations.register(coreRulesConfigName)
                project.dependencies.add(coreRulesConfigName, coreRulesMavenCoord)
                debugPrintln("${variant.name} => Using repository coreRules, ${coreRulesMavenCoord}")
            }

            mavenCoord.name = 'keystore'
            def keystoreConfiguration = project.configurations.register(keystoreConfigName)
            project.dependencies.add(keystoreConfigName, mavenCoord)
            debugPrintln("${variant.name} => Using repository keystore, ${mavenCoord}")

            // assembleXXX copies APK and mapping artifacts into output directory
            if (!project.takdevNoApp) {
                def assembleProvider = project.tasks.named("assemble${variant.name.capitalize()}")
                assembleProvider.configure({
                    doLast {
                        project.copy {
                            from apkZipConfiguration
                            into "${project.buildDir}/intermediates/atak-zips"
                            eachFile { fcd ->
                                def zipFileTree = project.zipTree(fcd.file)
                                def apkTree = zipFileTree.matching {
                                    include '**/*.apk'
                                }
                                def matcher = (apkTree.singleFile.name =~ /(.+-([a-zA-Z]+))\.apk/)
                                fcd.name = "${matcher[0][1]}.zip"
                                project.copy {
                                    from zipFileTree
                                    into "${project.buildDir}/outputs/atak-apks/${matcher[0][2]}"
                                    exclude "output-metadata.json"
                                }
                            }
                        }
                    }
                })
            }

            def mappingName = "proguard-${devFlavor}-${devType}-mapping.txt"
            def mappingFqn = "${project.buildDir}/${mappingName}"

            def preBuildProvider = project.tasks.named("pre${variant.name.capitalize()}Build")
            preBuildProvider.configure({
                doFirst {
                    // Proguard mapping; flavor specific
                    project.copy {
                        from mappingConfiguration
                        into project.buildDir
                        rename { sourceName ->
                            debugPrintln("${variant.name} => Copied proguard mapping ${sourceName} from repository into ${mappingFqn}")
                            return mappingName
                        }
                    }
                    System.setProperty("atak.proguard.mapping", mappingFqn)
                }
            })

            if (usesCoreMappingRules) {
                def coreRulesName = "proguard-release-keep.txt"
                def coreRulesFqn = "${project.buildDir}/${coreRulesName}"

                // add the proguard files up front, before task configuration
                project.android.buildTypes.release.proguardFile "${coreRulesFqn}"

                def preBuildProvider2 = project.tasks.named("pre${variant.name.capitalize()}Build")
                preBuildProvider2.configure({
                    doFirst {
                        // inject the actual rules file during configuration
                        project.copy {
                            from coreRulesConfiguration
                            into project.buildDir
                            rename { sourceName ->
                                debugPrintln("${variant.name} => Copied proguard keep rules ${sourceName} from repository into ${coreRulesFqn}")
                                return coreRulesName
                            }
                        }

                        // ATAK-19765+ATAK-19861
                        project.file("${coreRulesFqn}").append('\n-keepclassmembers class * implements com.atakmap.spi.PriorityServiceProvider { public int getPriority(); }')

                        println("augment the proguard file with the atak core rules")
                    }
                })
            }


            def signingClosure = {
                doFirst {
                    // Keystore
                    def storeName = 'android_keystore'
                    def storeFqn = "${project.buildDir}/${storeName}"
                    project.copy {
                        from keystoreConfiguration
                        into project.buildDir
                        rename { sourceName ->
                            debugPrintln("${variant.name} => Copied keystore from repository ${sourceName} into ${storeFqn}")
                            return storeName
                        }
                    }
                }
            }

            // inject keystore before validate signing
            ["validateSigning${variant.name.capitalize()}",
             "validateSigning${variant.name.capitalize()}AndroidTest"].each {
                try {
                    def signingProvider = project.tasks.named(it)
                    signingProvider.configure(signingClosure)
                } catch (UnknownTaskException ute) {
                    debugPrintln("Unknown Task, skippiing ${it}.")
                }
            }
        }
    }

    static VariantType getVariantType(Project project) {
        VariantType variantType = VariantType.UNKNOWN
        if (project.plugins.hasPlugin('com.android.application')) {
            variantType = VariantType.APPLICATION
        } else if (project.plugins.hasPlugin('com.android.library')) {
            variantType = VariantType.LIBRARY
        }
        return variantType
    }

    static DomainObjectSet getVariantSet(Project project) {
        DomainObjectSet variants
        switch (getVariantType(project)) {
            case VariantType.APPLICATION:
                variants = project.android.applicationVariants
                break
            case VariantType.LIBRARY:
                variants = project.android.libraryVariants
                break
            default:
                throw new GradleException('Cannot locate either application or library variants')
        }
        return variants
    }

    static String readPluginProperty(Project project, String name, String defaultValue) {
        File pluginConfig = project.rootProject.file('.takdev/plugin.properties')
        if (pluginConfig.exists()) {
            def localProperties = new Properties()
            FileInputStream fis = null
            try {
                fis = new FileInputStream(pluginConfig)
                localProperties.load(fis)
            } finally {
                if (fis != null)
                    fis.close()
            }
            def value = localProperties.get(name)
            if ((null != value)) {
                return value
            }
        }
        return defaultValue
    }

    static void writePluginProperty(Project project, String name, String value) {
        File pluginConfig = project.rootProject.file('.takdev/plugin.properties')
        if (!pluginConfig.exists())
            pluginConfig.getParentFile().mkdirs()
        def localProperties = new Properties()
        if (pluginConfig.exists()) {
            FileInputStream fis = null
            try {
                fis = new FileInputStream(pluginConfig)
                localProperties.load(fis)
            } finally {
                if (fis != null)
                    fis.close()
            }
        }
        if (null != value)
            localProperties.setProperty(name, value)
        else if (localProperties.containsKey(name))
            localProperties.remove(name)
        FileOutputStream fos = null
        try {
            fos = new FileOutputStream(pluginConfig)
            localProperties.store(fos, null)
        } finally {
            if (fos != null)
                fos.close()
        }
    }

    static String computeHash(Project p, MavenArtifactRepository repo) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256")
        def key = [
                repo.credentials.username,
                repo.credentials.password,
                repo.url,
                p.devkitVersion,
                p.snapshot
        ].join(':')
        def hash = digest.digest(key.getBytes(StandardCharsets.UTF_8))
        return Base64.encoder.encodeToString(hash)
    }

    static boolean tryResolve(Project p, MavenArtifactRepository takrepo, Map<?, ?> depcoord) {
        // This is done against the root project
        // to avoid polluting the configuration caching that occurs
        Project rootProject = p.rootProject

        // add the maven repo if it's not already installed
        if (rootProject.repositories.findByName(takrepo.name) == null) {
            rootProject.repositories.add(rootProject.repositories.mavenLocal())
            rootProject.repositories.add(takrepo)
        }

        // check the cached values for dependency resolution
        def depKey = "${depcoord['group']}.${depcoord['name']}"
        def repoHash = computeHash(p, takrepo)
        def dep = rootProject.dependencies.create(depcoord)
        def cachedHash = readPluginProperty(p, "${depKey}.hash", "")
        if (!cachedHash.equals(repoHash)) {
            // if the cached repo hash does not equal the hash for the
            // specified repo, clear the cached dependency resolution state
            writePluginProperty(p, "${depKey}.hash", null)
            writePluginProperty(p, "${depKey}.available", null)
        } else {
            // the hashes are equal, if dependency resolution is cached, return
            // the cached value
            String available = readPluginProperty(p, "${depKey}.available", null)
            if (available != null)
                return available.equals('true')
        }

        // create a transient configuration and attempt to resolve
        Configuration config = p.configurations.detachedConfiguration(dep)
        boolean resolved
        try {
            def deps = config.resolve()
            resolved = (null != deps) && !deps.empty
        } catch (Exception e) {
            // dependency resolution failed
            resolved = false
        }
        // update the cached dependency resolution state
        writePluginProperty(p, "${dep.name}.hash", repoHash)
        writePluginProperty(p, "${dep.name}.available", resolved ? 'true' : 'false')
        return resolved
    }

    static int[] splitVersionString(String version) {
        int[] components
        try {
            components = version.split('\\.').collect {
                it as Integer
            }
        } catch (Exception ex) {
            throw new GradleException("DevKit version, ${version}, is not a valid tuple - stopping build\n${ex.message}")
        }

        return components
    }

    // taken from https://gist.github.com/founddrama/971284 with changes
    static int versionComparator(String aStr, String bStr) {
        def VALID_TOKENS = /._/
        def a = aStr.tokenize(VALID_TOKENS)
        def b = bStr.tokenize(VALID_TOKENS)

        for (i in 0..<Math.max(a.size(), b.size())) {
            if (i == a.size()) {
                return b[i].isInteger() ? -1 : 1
            } else if (i == b.size()) {
                return a[i].isInteger() ? 1 : -1
            }

            if (a[i].isInteger() && b[i].isInteger()) {
                int c = (a[i] as int) <=> (b[i] as int)
                if (c != 0) {
                    return c
                }
            } else if (a[i].isInteger()) {
                return 1
            } else if (b[i].isInteger()) {
                return -1
            } else {
                int c = a[i] <=> b[i]
                if (c != 0) {
                    return c
                }
            }
        }
        return 0
    }

    static String getDesiredTpcFlavorName(Object variant, Boolean fallback) {
        String flavorName = variant.flavorName ?: 'civ'
        def matchingFallbacks = variant.productFlavors.matchingFallbacks
        if (fallback &&
                !matchingFallbacks.isEmpty() &&
                !matchingFallbacks[0].isEmpty()) {
            flavorName = matchingFallbacks[0][0]
        }
        return flavorName
    }


}
