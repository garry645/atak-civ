package com.atak.lint.test

import java.io.File
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.atak.lint.checks.AndroidUtilLogDetector.Companion.AndroidUtilLogIssue
import org.junit.Test

class AndroidUtilLogDetectorTest {
    @Test
    fun usingAndroidLogWithTwoArgumentsAndImport() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |import android.util.Log;
                |public class Example {
                |  public void log() {
                |    Log.d("TAG", "msg");
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |import android.util.Log
                |class Example {
                |  fun log() {
                |    Log.d("TAG", "msg")
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expect(
                """
            |src/foo/Example.java:2: Warning: Use ATAK's log package instead. [AndroidUtilLog]
            |import android.util.Log;
            |~~~~~~~~~~~~~~~~~~~~~~~~
            |src/foo/Example.kt:2: Warning: Use ATAK's log package instead. [AndroidUtilLog]
            |import android.util.Log
            |~~~~~~~~~~~~~~~~~~~~~~~
            |0 errors, 2 warnings
            |""".trimMargin()
            )
            .expectFixDiffs(
                """
            |Fix for src/foo/Example.java line 2: Replace with com.atakmap.coremap.log.Log:
            |@@ -2 +2
            |- import android.util.Log;
            |+ import com.atakmap.coremap.log.Log;
            |Fix for src/foo/Example.kt line 2: Replace with com.atakmap.coremap.log.Log:
            |@@ -2 +2
            |- import android.util.Log
            |+ import com.atakmap.coremap.log.Log
            |""".trimMargin()
            )
    }

    @Test
    fun usingAndroidLogWithThreeArgumentsAndImport() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |import android.util.Log;
                |public class Example {
                |  public void log() {
                |    Log.d("TAG", "msg", new Exception());
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |import android.util.Log
                |class Example {
                |  fun log() {
                |    Log.d("TAG", "msg", Exception())
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expect(
                """
            |src/foo/Example.java:2: Warning: Use ATAK's log package instead. [AndroidUtilLog]
            |import android.util.Log;
            |~~~~~~~~~~~~~~~~~~~~~~~~
            |src/foo/Example.kt:2: Warning: Use ATAK's log package instead. [AndroidUtilLog]
            |import android.util.Log
            |~~~~~~~~~~~~~~~~~~~~~~~
            |0 errors, 2 warnings
            |""".trimMargin()
            )
            .expectFixDiffs(
                """
            |Fix for src/foo/Example.java line 2: Replace with com.atakmap.coremap.log.Log:
            |@@ -2 +2
            |- import android.util.Log;
            |+ import com.atakmap.coremap.log.Log;
            |Fix for src/foo/Example.kt line 2: Replace with com.atakmap.coremap.log.Log:
            |@@ -2 +2
            |- import android.util.Log
            |+ import com.atakmap.coremap.log.Log
            |""".trimMargin()
            )
    }

    @Test
    fun usingFullyQualifiedAndroidLogWithTwoArguments() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |public class Example {
                |  public void log() {
                |    android.util.Log.d("TAG", "msg");
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |class Example {
                |  fun log() {
                |    android.util.Log.d("TAG", "msg")
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expect(
                """
            |src/foo/Example.java:4: Warning: Use ATAK's log method instead. [AndroidUtilLog]
            |    android.util.Log.d("TAG", "msg");
            |    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |src/foo/Example.kt:4: Warning: Use ATAK's log method instead. [AndroidUtilLog]
            |    android.util.Log.d("TAG", "msg")
            |    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |0 errors, 2 warnings""".trimMargin()
            )
            .expectFixDiffs(
                """
            |Fix for src/foo/Example.java line 4: Replace with com.atakmap.coremap.log.Log.d("TAG", "msg"):
            |@@ -4 +4
            |-     android.util.Log.d("TAG", "msg");
            |+     com.atakmap.coremap.log.Log.d("TAG", "msg");
            |Fix for src/foo/Example.kt line 4: Replace with com.atakmap.coremap.log.Log.d("TAG", "msg"):
            |@@ -4 +4
            |-     android.util.Log.d("TAG", "msg")
            |+     com.atakmap.coremap.log.Log.d("TAG", "msg")
            |""".trimMargin()
            )
    }

    @Test
    fun usingFullyQualifiedAndroidLogWithThreeArguments() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |public class Example {
                |  public void log() {
                |    android.util.Log.d("TAG", "msg", new Exception());
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |class Example {
                |  fun log() {
                |    android.util.Log.d("TAG", "msg", Exception())
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expect(
                """
            |src/foo/Example.java:4: Warning: Use ATAK's log method instead. [AndroidUtilLog]
            |    android.util.Log.d("TAG", "msg", new Exception());
            |    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |src/foo/Example.kt:4: Warning: Use ATAK's log method instead. [AndroidUtilLog]
            |    android.util.Log.d("TAG", "msg", Exception())
            |    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            |0 errors, 2 warnings""".trimMargin()
            )
            .expectFixDiffs(
                """
            |Fix for src/foo/Example.java line 4: Replace with com.atakmap.coremap.log.Log.d("TAG", "msg", new Exception()):
            |@@ -4 +4
            |-     android.util.Log.d("TAG", "msg", new Exception());
            |+     com.atakmap.coremap.log.Log.d("TAG", "msg", new Exception());
            |Fix for src/foo/Example.kt line 4: Replace with com.atakmap.coremap.log.Log.d("TAG", "msg", Exception()):
            |@@ -4 +4
            |-     android.util.Log.d("TAG", "msg", Exception())
            |+     com.atakmap.coremap.log.Log.d("TAG", "msg", Exception())
            |""".trimMargin()
            )
    }

    @Test
    fun usingFullyQualifiedAndroidLogGetStackTraceString() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |public class Example {
                |  public void log() {
                |    android.util.Log.getStackTraceString(new Exception());
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |class Example {
                |  fun log() {
                |    android.util.Log.getStackTraceString(Exception())
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expectClean()
    }

    @Test
    fun usingImportedAndroidLogGetStackTraceString() {
        lint()
            .files(
                java(
                    """
                |package foo;
                |import android.util.Log;
                |public class Example {
                |  public void log() {
                |    Log.getStackTraceString(new Exception());
                |  }
                |}""".trimMargin()
                ),
                kotlin(
                    """
                |package foo
                |import android.util.Log
                |class Example {
                |  fun log() {
                |    Log.getStackTraceString(Exception())
                |  }
                |}""".trimMargin()
                )
            )
            .issues(AndroidUtilLogIssue)
            .run()
            .expectClean()
    }
}