package com.android.tools.lint.checks

import com.android.SdkConstants
import com.android.ide.common.repository.GradleCoordinate
import com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_HIGHER
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintUtils
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UClass

/**
 * Constructs a new [MediaBrowserServiceCompatVersionDetector] check
 */
class MediaBrowserServiceCompatVersionDetector : Detector(), Detector.UastScanner {

    companion object Issues {

        @JvmField val ISSUE = Issue.create(
                "IncompatibleMediaBrowserServiceCompatVersion",
                "Obsolete version of MediaBrowserServiceCompat",
                """
`MediaBrowserServiceCompat` from version 23.2.0 to 23.4.0 of the Support v4 Library \
used private APIs and will not be compatible with future versions of Android beyond Android N.\
Please upgrade to version 24.0.0 or higher of the Support Library.""",
                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                Implementation(
                        MediaBrowserServiceCompatVersionDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                ))

        /**
         * Minimum recommended support library version that has the necessary fixes
         * to ensure that MediaBrowserServiceCompat is forward compatible with N
         */
        val MIN_SUPPORT_V4_VERSION: GradleCoordinate = GradleCoordinate.parseVersionOnly("24.0.0")

        const val MEDIA_BROWSER_SERVICE_COMPAT = "android.support.v4.media.MediaBrowserServiceCompat"
    }

    override fun applicableSuperClasses(): List<String>? {
        return listOf(MEDIA_BROWSER_SERVICE_COMPAT)
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (!context.evaluator.extendsClass(declaration,
                MEDIA_BROWSER_SERVICE_COMPAT, true)) {
            return
        }

        val dependencies = GradleDetector.getCompileDependencies(context.project) ?: return
        for (library in dependencies.libraries) {
            val mc = library.resolvedCoordinates
            @Suppress("SENSELESS_COMPARISON")
            if (mc != null
                    && mc.groupId == SdkConstants.SUPPORT_LIB_GROUP_ID
                    && mc.artifactId == "support-v4"
                    && mc.version != null) {
                val libVersion = GradleCoordinate.parseVersionOnly(mc.version)
                if (COMPARE_PLUS_HIGHER.compare(libVersion, MIN_SUPPORT_V4_VERSION) < 0) {

                    val location = LintUtils.guessGradleLocation(context.client, context.project.dir,
                            "${mc.groupId}:${mc.artifactId}:${mc.version}")

                    val message = "Using a version of the class that is not forward compatible"
                    context.report(ISSUE, location, message)
                }
                break
            }
        }
    }
}
