/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atak.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.util.isMethodCall


/**
 *
 */
class JavaFileIODetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement?>>? {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val psiMethod = node.resolve()
                val evaluator = context.evaluator
                if (psiMethod != null && (
                        evaluator.isMemberInClass(psiMethod, "java.io.File") ||
                        evaluator.isMemberInClass(psiMethod, "java.io.FileInputStream") ||
                        evaluator.isMemberInClass(psiMethod, "java.io.FileOutputStream") ||
                        evaluator.isMemberInClass(psiMethod, "java.io.RandomAccessFile") ||
                        evaluator.isMemberInClass(psiMethod, "java.nio.FileChannel"))) {
                    if (!(!node.isMethodCall() && evaluator.isMemberInClass(psiMethod, "java.io.File"))) {
                        context.report(JavaFileIOIssue, node, context.getLocation(node), "Avoid direct Java File IO API use")
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Issue describing the problem and pointing to the detector
         * implementation.
         */
        @JvmField
        val JavaFileIOIssue: Issue = Issue.create(
            // ID: used in @SuppressLint warnings etc
            id = "JavaFileIOIssue",
            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            briefDescription = "Direct Java File IO API use",
            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            explanation = """
                Please do not use directly use any of the following Java File IO APIs:
                    java.io.File (any method other than the constructor)
                    java.io.FileInputStream
                    java.io.FileOutputStream
                    java.io.RandomAccessFile
                    java.nio.FileChannel
                Please use the IOProviderFactory API
                    """,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                JavaFileIODetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
