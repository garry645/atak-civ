from conans import ConanFile, AutoToolsBuildEnvironment, tools
from conans.tools import load
import os
import re

class CesiumNativeConan(ConanFile):
    name = "cesium-native"
    description = "Cesium Native is a set of C++ libraries for 3D geospatial"
    settings = "os", "compiler", "build_type", "arch"

    def set_version(self):
        self.version = os.environ["CESIUM_NATIVE_REF"]

    def package(self):
        source = self._get_source()

        print(source)

        self.copy("*/*", dst=source + "/include", src="build/" + source + "/include")
        self.copy("async++.h", dst=source + "/include", src="build/" + source + "/include")
        self.copy("*.a", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.so", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.dylib", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.dll", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.lib", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.pdb", dst=source + "/lib", src="build/" + source + "/lib")
        self.copy("*.dll", dst=source + "/lib/debug", src="build/" + source + "/lib/debug")
        self.copy("*.lib", dst=source + "/lib/debug", src="build/" + source + "/lib/debug")
        self.copy("*.pdb", dst=source + "/lib/debug", src="build/" + source + "/lib/debug")
        self.copy("*.a", dst=source + "/lib", src="build/" + source + "/lib64")
        self.copy("*.pc", dst=source + "/lib", src="build/" + source + "/lib64")

    def package_info(self):
        source = self._get_source()
        self.cpp_info.libdirs = [source+"/lib"]
        self.cpp_info.includedirs = [source+"/include"]

    def _get_source(self):
        source = ""
        if self.settings.os == "Windows":
            source += "windows-"
            if self.settings.arch == "x86_64":
                source += "x86_64"
            else:
                source += "x86"
        elif self.settings.os == "Android":
            source += "android-"
            if self.settings.arch == "armv8":
                 source += "arm64-v8a"
            elif self.settings.arch == "armv7":
                 source += "armeabi-v7a"
            elif self.settings.arch == "x86":
                 source += "x86"
            elif self.settings.arch == "x86_64":
                 source += "x86_64"
            else:
                raise Exception("Architecture not recognized")
        elif self.settings.os == "Macos":
            source = "macos-x86_64"
        elif self.settings.os == "Linux":
            source = "linux-x86_64"
        return source
