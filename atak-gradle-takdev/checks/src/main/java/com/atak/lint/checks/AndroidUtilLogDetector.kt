package com.atak.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.LintMap
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UImportStatement

/**
 * Class to detect usages of the android.util.Log package and produce a lint warning with a quick
 * fix to replace the package with ATAK's log package: com.atakmap.coremap.log.Log.
 */
class AndroidUtilLogDetector : Detector(), SourceCodeScanner {
    /**
     * These are the methods that are in ATAK log and Android log.
     */
    override fun getApplicableMethodNames() =
        listOf("v", "d", "i", "w", "e", "wtf", "println")

    override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        if (evaluator.isMemberInClass(method, ANDROID_UTIL_LOG_PACKAGE)) {
            val lintMap = context.getPartialResults(AndroidUtilLogIssue).map()
            var methodMap = lintMap.getMap(METHOD_MAP_KEY)
            if (methodMap == null) {
                methodMap = LintMap()
                lintMap.put(METHOD_MAP_KEY, methodMap)
            }
            methodMap.put(
                context.getLocation(node).toString(), Incident(
                    issue = AndroidUtilLogIssue,
                    scope = node,
                    location = context.getLocation(node),
                    message = "Use ATAK's log method instead.",
                    fix = quickFixIssueLog(node)
                )
            )
        }
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                if (node.asRenderString() == "import $ANDROID_UTIL_LOG_PACKAGE") {
                    val lintMap = context.getPartialResults(AndroidUtilLogIssue).map()
                    var importMap = lintMap.getMap(IMPORT_MAP_KEY)
                    if (importMap == null) {
                        importMap = LintMap()
                        lintMap.put(IMPORT_MAP_KEY, importMap)
                    }
                    importMap.put(
                        context.getLocation(node).toString(), Incident(
                            issue = AndroidUtilLogIssue,
                            scope = node,
                            location = context.getLocation(node),
                            message = "Use ATAK's log package instead.",
                            fix = quickFixIssueLogImport()
                        )
                    )
                }
            }
        }
    }

    override fun afterCheckFile(context: Context) {
        val lintMap = context.getPartialResults(AndroidUtilLogIssue).map()
        val methodMap = lintMap.getMap(METHOD_MAP_KEY)
        val importMap = lintMap.getMap(IMPORT_MAP_KEY)

        /**
         * If there are import usage warnings and method usage warnings, only report the import
         * usage warnings, as changing the import will satisfy the method usage warnings.
         *
         * If there are method usage warning and no import usage warnings, then just report the
         * method usage warnings.
         */
        if (methodMap != null && importMap != null) {
            for (key in importMap) {
                val incident = importMap.getIncident(key)
                context.report(incident!!)
            }
        } else if (methodMap != null) {
            for (key in methodMap) {
                val incident = methodMap.getIncident(key)
                context.report(incident!!)
            }
        }

        lintMap.remove(METHOD_MAP_KEY)
        lintMap.remove(IMPORT_MAP_KEY)
    }

    private fun quickFixIssueLog(logCall: UCallExpression): LintFix {
        val arguments = logCall.valueArguments
        val methodName = logCall.methodName

        var fixSource = "$ATAK_LOG_PACKAGE."

        when (arguments.size) {
            2 -> {
                val arg1 = arguments[0]
                val arg2 = arguments[1]
                fixSource += "$methodName(${arg1.asSourceString()}, ${arg2.asSourceString()})"
            }

            3 -> {
                val arg1 = arguments[0]
                val arg2 = arguments[1]
                val arg3 = arguments[2]
                fixSource += "$methodName(${arg1.asSourceString()}, ${arg2.asSourceString()}, ${arg3.sourcePsi?.text})"
            }

            else -> {
                throw IllegalStateException("$ANDROID_UTIL_LOG_PACKAGE overloads should have 2 or 3 arguments")
            }
        }

        val logCallSource = logCall.uastParent!!.sourcePsi?.text
        return fix().replace().text(logCallSource).with(fixSource).shortenNames().reformat(true)
            .build()
    }

    private fun quickFixIssueLogImport(): LintFix {
        return fix().replace().pattern(ANDROID_UTIL_LOG_PACKAGE).with(ATAK_LOG_PACKAGE).shortenNames().reformat(true)
            .build()
    }

    companion object {
        const val ANDROID_UTIL_LOG_PACKAGE = "android.util.Log"
        const val ATAK_LOG_PACKAGE = "com.atakmap.coremap.log.Log"
        const val METHOD_MAP_KEY = "MethodMap"
        const val IMPORT_MAP_KEY = "ImportMap"

        @JvmField
        val AndroidUtilLogIssue: Issue = Issue.create(
            id = "AndroidUtilLog",
            briefDescription = "Android Log utility use",
            explanation = "ATAK plugins should use '$ATAK_LOG_PACKAGE' when logging.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                AndroidUtilLogDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}