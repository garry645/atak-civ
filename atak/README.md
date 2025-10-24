# Overview

ATAK is a Moving Map capability for the Android OS. The moving map capability is based on a core civilian capability that is built first and then flavored with either the military or fms capabilities.  Full development resources can be found at wiki.tak.gov.

ATAK is compiled to use compileSdkVersion 34, targetSdkVersion 34 but still provide support for the minSdkVersion 21.    Be careful when developing new code within core to make sure that appropriate safeguards are in place that retain system compatibility for minSdkVersion 21.


# Developer Info

Information relevant to current ATAK development is below.

For version-to-version changes and in-depth notes,
 please see the following (if accessible):
[tak wiki](https://wiki.tak.gov/display/DEV/ATAK+Development)


## Required Development Tools

| Tool | Minimum or Required Version | Recommended Version |
| ---  | --- | --- |
| JDK | 17 (Required) | [OpenJDK (Temurin) version 17](https://adoptopenjdk.net/) |
| Android SDK | - | - |
| Android NDK | 25b | 25b |
| CMake | 3.22.1 | 3.25.3 |
| conan | 1.59.0 (Required - 2.x versions will not work) | 1.59.0 |
| git client | 2.19 | 2.39.2 |
| git-lfs | 3.0.2 | 3.2.0 |


## Required Developer Configuration/Setup

ATAK re-uses several build configuration items used by takkernel. 
Developers must specify several local configuration parameters before a
build can be done.  This is done by creating a local.properies file
containing your local parameters; this local.properties should be placed in
the root of the atak repository and the ATAK subfolder and the FlavorPlugin
subfolder.

A sample local.properties file, local.properties.sample, is given as a
starting point to create your own local.properties. Specific details on
the configuration elements, as well as known issues, can be found in the
takkernel build setup page here: [native libraries](https://wiki.tak.gov/display/COREDEV/Building+Native+Libraries)
Note that any development tool version requirements specified on the above-linked
page for takkernel setup is superceded by the ATAK requirements specified in
table of the previous section of this document.

Failure to properly configure local.properties as described above 
will result in the following error message:

	> FAILURE: Build failed with an exception.
	> * What went wrong:
	> Execution failed for task ':app:mergeCivDebugResources'.
	Could not resolve all files for configuration ':app:civDebugRuntimeClasspath'.
	>    > Could not resolve gov.tak.kernel:takkernel-aar:0.0.0.
	>      Required by:
	>          project :app
	>          project :app > project :ATAKMapEngine:lib
	>       > Could not resolve gov.tak.kernel:takkernel-aar:0.0.0.
	>          > Could not get resource 'http://localhost/gov/tak/kernel/takkernel-aar/0.0.0/takkernel-aar-0.0.0.pom'.
	>             > Could not GET 'http://localhost/gov/tak/kernel/takkernel-aar/0.0.0/takkernel-aar-0.0.0.pom'.
	>                > Connect to localhost:80 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused: connect

## TAK Kernel

By default, a strongly versioned (see gradle/versions.gradle) binary build
of takkernel is used during ATAK core build.  

If the takkernel source tree (clone of https://git.tak.gov/core/takkernel)
exists as a sibling to the atak top level directory, then the ATAK build
process will additionally try to build and use the takkernel sources
there-in.  This "build from source" behavior can be overridden by setting
the takkernel.dependency.forceRemote property to true in your
local.properties; this will force the use of the strongly versioned binary
version as described at the top of this section regardless of if a takkernel
source tree is present.

Additional tools and setup are required to build TAK Kernel; see
the [TAK Kernel build setup page](https://wiki.tak.gov/display/COREDEV/Building+Native+Libraries)

## Building

Baseline build capability is via gradle on the command line. Some common
gradle targets for ATAK are listed below (invoke via ./gradlew <target>):

* assembleCivDebug - build CIV variant, debug version, of ATAK
* assembleMilDebug - build MIL variant, debug version, of ATAK
* installCivDebug - build CIV variant and install to connected device
* installMilDebug - build MIL variant and install to connected device

## Code Formatting and Style

* Android Studio does suggest replacing Anonymous Inner classes with Lambdas - please do not do this.

* There is a code formatting that is provided with the repository and is discussed under the Formatting section.

* Repository development should make use of the model as outlined by 
  [branching model](https://nvie.com/posts/a-successful-git-branching-model/) which closely matches the WinTAK 
  development model.

* All development should make use of the android-formatting.prefs rules, 
  use LF (Unix) breaks, and should contain sufficient documentation to 
  be used by development team. 

* Prior to code review or periodically, your code will be stripped of CR's and hard tabs.   It is also a good idea to make use of the code formatting files  in the root of the directory.   Automatic code formatting is run on occasion.
              
* Consideration of third party libraries need to occur early in a release cycle. Without proper attribution and appropriate review, third party libraries will be removed.



## Using an Emulator

When using an emulator, ATAK might crash due to incompatibilities with the computers graphics card. A few potential work arounds are:

1. construct a file in the /sdcard/atak directory called "opengl.broken"
   - this will set USE_GENERIC_EGL_CONFIG true 
1. set the system property "USE_GENERIC_EGL_CONFIG" to  "true"
1. If all else fails modify code 
./ATAKMapEngine/src/com/atakmap/map/opengl/GLMapSurface.java:129

    if (System.getProperty("USE_GENERIC_EGL_CONFIG", "false").equals("true"))
to be:
    if (true)

If EGL is activated successfully, you should see a log message stating: "application has been informed that OPEN GL is a bit busted"

If using AVD, it may be necessary to switch the GPU mode. This can be done by accessing the Settings for your emulated device, then going to the Settings tab, then Advanced tab. From here you can select the OpenGL ES Renderer, which is the setting that may need to be changed. The emulated device will need to be cold booted for the change to be effective. If all else fails, AVD may be launched from the command line and passed the -gpu flag with various options--"host" and "guest" are likely the most helpful.



# Android Studio

## Version Information

| Minimum | Recommended |
| --- | --- |
| 2024.2.2 Patch 1 "LadyBug" | 2024.2.2 Patch 1 "LadyBug" |

Please note that when you do update to LadyBug you will need to go into File->Settings and select Build Execution Deployment->Build Tools->Gradle and select the Gradle JDK to be 17.0.X on a per project basis.  Otherwise you will get this error when trying to sync:


    Your build is currently configured to use incompatible Java 21.0.3 and
     Gradle 8.0. Cannot sync the project.
     
     We recommend upgrading to Gradle version 8.9.
     
     The minimum compatible Gradle version is 8.5.
     
     The maximum compatible Gradle JVM version is 19.
     
     Possible solutions:
      - Upgrade to Gradle 8.9 and re-sync
      - Upgrade to Gradle 8.5 and re-sync

After the upgrade you may need to go into Build Variants and select 'Re-import with defaults'.

## Launch Options

ATAK *requires* core developers using Android Studio to manually specify
the activity to launch in Android Studio Launch Options.
This is because of how ATAK has multiple default intents of which only one
is enabled based on if you are running the "flavored" or "unflavored"
version of ATAK;  this confuses Android Studio's detection of the "default"
activity to launch.

Under the Run/Debug configuration for ATAK, please set the
Launch Activity to com.atakmap.app.ATAKActivityCiv if you are running
without the MIL "Flavor" installed on the device.
If have the MIL "Flavor" installed, set the activity to
com.atakmap.app.ATAKActivityMil

On devices running Android versions less than 13, you can simply set the
launch activity to com.atakmap.app.ATAKActivity regardless of if you have
the MIL "Flavor" installed or not.

If you do not set the Launch Activity and leave it as default,
Android Studio will fail to launch ATAK with a message similar to the
following:

    $ adb shell am start -n "com.atakmap.app.civ/com.atakmap.app.ATAKActivityCiv" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    Error while executing: am start -n "com.atakmap.app.civ/com.atakmap.app.ATAKActivityCiv" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=com.atakmap.app.civ/com.atakmap.app.ATAKActivityCiv }
    Error type 3
    Error: Activity class {com.atakmap.app.civ/com.atakmap.app.ATAKActivityCiv} does not exist.

## Formatting

Source code within the ATAK repository is formatted using a set of Eclipse rules that exist in the
root of the repository.   In order to use these rules with Android Studio, we require the installation
of the EclipseCodeFormatter plugin.   Once installed, the required configuration files are all set up.
Within Android Studio

   With newer versions of Android Studio you may need to install it from a file [code formatter](https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter)

   Settings->
          Plugins->
              Type in Eclipse in the Search Box
              Tap Browse Repository Button
              Select the Eclipse Code Formatter Plugin 
       Once Android Studio restarts, then you should be able to perform normal code formatting per the 
       ATAK formatting rules.




# Plugins

Plugins for ATAK are additional Android Applications that are built but cannot be run stand alone.   
These plugins are built so that at runtime they rely on the internal classes within the ATAK 
application.  This is done through a special classloader that knows how to join up the missing 
class files from the plugin application at run time.    During compile time, the plugin only needs
to reference the classes using the provided keyword.

The plugin architecture is no different than developing code internally to ATAK.   
In fact, all ATAK components use the same constructs and capabilities.    There is 
not a single document that could cover the entire capabilities of the plugin
architecture, but there are several examples that show how to do basic capabilities.


## HelloWorld Plugin [project](https://git.tak.gov/samples/helloworld)

A good example of a plugin that does several different tasks is the Helloworld plugin.  
This plugin is structured to be very minimalistic and contains a single user experience. 
The plugin describes the capabilities within the assets/plugin.xml file under the 
plugins/helloworld directory. The java src code contains comment to guide the developer 
through the mechanics of how a plugin works.
 
## Plugin Structure

The assets/plugin.xml contains the information required to load a plugin.
Plugins may implement Lifecycle and/or Tools.   In the helloworld example, the 
plugin.xml file contains both.

By convention the lifecycle and tool are mostly boilerplate code and can be 
mostly duplicated from one project to another.   In the ATAK architecture, a 
Lifecycle closely relates to a MapComponent.  A Tool is used to populate the 
action bar and describe the intent that is fired by invoking the icon on the 
actionbar.

When developing in the plugin architecture, it is important to remember that 
there are two context's in use.   The primary context is the ATAK context and
should be used when visual components such as AlertDialog is being constructed.   
The secondary context is the plugin's context and should be used when looking up
resources and graphics specific to the plugin.  Keep this in mind during 
development.   Using the wrong context can lead to runtime crashes or wrong 
visual behavior.

A few notes:

1) Notifications cannot reference a resource from the plugin.  The implementation 
of the Notification class is not capable of realizing where to look up a resource.
In the helloworld plugin, there is an example of how to have your plugin display an
appropriate small icon.

2) Spinners will crash on devices depending how the theme is set.   For this purpose,
ATAK provides a com.atakmap.android.gui.PluginSpinner class which is identical 
to a real android Spinner.

3) Signing a Plugin -  If you choose to develop a plugin, the key alogrithm must be RSA and the 
   Signing Algorithm must be SHA1withRSA.

4) Some plugins will fail to compile using gradle with an obscure ndkStrip error when it contains 
   no native libraries.   Please unset ANDROID_NDK_HOME and/or move it out of the ANDROID_SDK_HOME/NDK directory.
   This is an issue with the gradle build system at this time. The specific error message is:
    :transformNativeLibsWithStripDebugSymbolForDebug FAILED
    
    FAILURE: Build failed with an exception.
    
    * What went wrong:
    Execution failed for task ':transformNativeLibsWithStripDebugSymbolForDebug'.
    > java.lang.NullPointerException (no error message)


## Plugin Debugging

Plugins are not processes.   When deployed to the device, they are just containers that are pulled 
into the ATAK process space.  
1. Set corresponding break points within the plugin code.
1. You will need to direct Android Studio to attach to an existing process.
     Under the Run menu
        Attach debugger to existing process.
        Show all process
        Select com.atakmap.app.
1. Once that has been done, you should see when your breakpoint is hit, 
   the debugger will stop appropriately.

## Playstore Publication

Playstore publication should be done with the civSmall apk.   This apk also needs the mapping file pulled from the tak.gov build pipeline and the native-debug-symbols file produced as well.

Please note the guidance from https://support.google.com/googleplay/android-developer/answer/9848633?hl=en#zippy=%2Cnative-generate-a-debug-symbols-file

If your project builds an APK, use the build.gradle build setting above to generate the debug symbols file separately. Manually upload the debug symbols file to the Google Play Console as described below in Step 2: Upload a deobfuscation or symbolication file. As part of the build process, the Android Gradle plugin outputs this file in the following project location: app/build/outputs/native-debug-symbols/variant-name/native-debug-symbols.zip

# Useful Information

* Filing a bug with a screen capture.    
     There are many ways to capture the screen for bug submission.  By default
     a large png file is produced.   If you have access to adb and would like 
     to capture directly to a jpg, you can execute:

      unix - 
        adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png
      windows - 
        adb shell screencap -p /sdcard/screen.png 
        adb pull /sdcard/screen.png

* To record a video from the command line of the screen.
      adb shell screenrecord /sdcard/demo.mp4
      adb pull /sdcard/demo.mp4

     Stop the screen recording by pressing Ctrl-C, 
     otherwise the recording stops automatically at 
     three minutes or the time limit set by --time-limit.

* To print out the certificate for the a specific APK
     unzip -p Name-of-apk.apk META-INF/CERT.RSA | keytool -printcert

       - or -

     unzip -p Name-of-apk.apk META-INF/CERT.DSA | keytool -printcert

       - or -

     keytool -printcert -jarfile Name-of-apk.apk

* To sign a apk with multiple certificates or to strip the certificate and add a new one 

      - 26.0.2 build-tools apksigner allows for multiple key stores to be specified without using keystore
        rotation (but both keystores are required to be known)

          $ANDROID_SDK/./build-tools/26.0.2/apksigner sign --ks keystore1 --next-signer --ks keystore2
      
      - apksigner allows for existing keystore to be stripped and a new one added

          $ANDROID_SDK/./build-tools/26.0.2/apksigner sign --ks keystore1 

      - the latest apksigner requires multiple keystores to be treated as a app signature rotation which is 
        stored different within the file
      

* Since ATAK has now migrated to crypted databases I have been getting questions on how to examine them for debugging purposes.   Please see https://github.com/sqlitebrowser/sqlitebrowser/releases      This allows for you to decrypt the databases if you know your passphrase.

* Go to File -> Settings or press CTRL + ALT + S . The following window will open and check Show quick doc on mouse move under IDE Settings -> Editor. Or just press CTRL and hover your move over your method, class ... If you just need a shortcut, then it is Ctrl + Q on Linux (and Windows).

* To execute a gitlab-runner locally 
         
         gitlab-runner exec docker sdk 

  NOTE: If your plugin makes use of git-lfs and you are trying to run it locally

         gitlab-runner exec docker --pre-clone-script "git config --global lfs.url https://gitlab-ci-token:<your token>@repo-url/info/lfs" assembleMilRelease


* Testing your app with App Standby

To test the App Standby mode with your app:

    Configure a hardware device or virtual device with an Android 6.0 (API level 23) or higher system image.
    Connect the device to your development machine and install your app.
    Run your app and leave it active.
    Force the app into App Standby mode by running the following commands:

    $ adb shell dumpsys battery unplug
    $ adb shell am set-inactive com.atakmap.app true

    Simulate waking your app using the following commands:

    $ adb shell am set-inactive com.atakmap.app false
    $ adb shell am get-inactive com.atakmap.app

    Observe the behavior of your app after waking it. Make sure the app recovers gracefully from standby mode. In particular, you should check if your app's Notifications and background jobs continue to function as expected. 


* Shrinking and removing EXIF data from a TAK PDF

    $ gs -dNOPAUSE -dBATCH -sDEVICE=pdfwrite -dCompatibilityLevel=1.7 -dPDFSETTINGS=/ebook -sOutputFile=output.pdf $1

* Pulling ANR trace logs depending on the device

   Older Devices
    $ adb pull /data/anr/traces.txt 

   Newer Devices (Specifically Samsung) 
      1) go into the dialer and type in *#9900#
            for a tablet without a dialer - 
                 In the calculator, enter
                       (+30012012732+
                 Then enter *#9900# in the menu that opens.
      2) select "Run Dumpstate/Logcat/Modem Log"
      3) when that finishes select "Copy To SDCARD (Include CP Ramdump)"
            - please note that you do not need a sd card, this is just what it is called 
              on the screen and really refers to the directory that Android uses for its
              internal memory
      4) the directory under /sdcard/logs should contain a ton of things including the traces 
         or a sub directory should contain the traces
            - the 'logs' directory should show up as the internal memory if you are browsing 
              to it using Windows explorer
      5) zip up the logs directory 
      6) go back into *#9900# and clear the logs

    Pixel devices
     $ adb bugreport

  * Overide of screen timeout defaults (lasts until reboot)

     adb shell settings put system screen_off_timeout 1800000
     adb shell settings get system screen_off_timeout


* Utilize a local version of the dev kit for building atak
    
     git clone https://git.tak.gov/tools/atak-gradle-devkit
     cd atak-gradle-devkit
     # modify build.gradle buildscript.version
         ...
     # modify impl as desired
         ...
     # publish modified version to maven local
     ./gradlew publishToMavenLocal

     # repeat last two steps as desired
         ...


* Test Coverage Reports

For TAK Kernel:

Go to Pipelines
Find one that has completed the testLinux64 phase
Download testLinux64:archive artifacts
Open artifacts.zip\build\java\reports\jacoco\jacocoRootReport\jacocoRootReport.xml and parse however you wish
For ATAK:

Go to Pipelines
Find one that has completed the connectedAndroidTest phase
Download connectedAndroidTest:archive artifacts
Open artifacts.zip\ATAK\app\build\reports\jacoco\createCivDebugJacocoTestReport\createCivDebugJacocoTestReport.xml and parse however you wish

