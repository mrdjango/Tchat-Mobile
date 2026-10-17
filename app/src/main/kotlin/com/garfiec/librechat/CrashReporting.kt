package com.garfiec.librechat

import android.app.Application
import com.garfiec.librechat.core.brand.TchatBrand
import io.sentry.android.core.SentryAndroid

/**
 * Crash and ANR reporting to GlitchTip (Sentry-compatible), project `tchat-mobile` at
 * glitchtip.tensorgrid.space. Started first thing in [LibreChatApplication.onCreate] so a failure
 * while Koin starts is reported too; Sentry chains to the previous uncaught-exception handler, and
 * the diagnostics handler installed later chains to Sentry's, so the process still crashes as
 * before.
 *
 * Privacy: no user id, IP, request data, screenshots, view hierarchy, touch breadcrumbs or
 * tracing. Events carry the stack trace, device/OS model and app version.
 */
internal fun Application.initCrashReporting() {
    val dsn = TchatBrand.CRASH_REPORTING_DSN
    if (dsn.isBlank()) return
    SentryAndroid.init(this) { options ->
        options.dsn = dsn
        options.environment = if (BuildConfig.DEBUG) "debug" else "production"
        options.release = "tchat-mobile@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
        options.isSendDefaultPii = false
        options.isAttachScreenshot = false
        options.isAttachViewHierarchy = false
        options.isEnableUserInteractionBreadcrumbs = false
        options.isEnableUserInteractionTracing = false
        // GlitchTip has no release-health view, so session pings would only cost battery.
        options.isEnableAutoSessionTracking = false
        options.tracesSampleRate = null
        options.setBeforeSend { event, _ ->
            event.user = null
            event.request = null
            event
        }
    }
}
