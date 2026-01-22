package com.looker.droidify.ui.appDetail

import android.content.Context
import com.looker.droidify.R

enum class LinkType(
    val iconResId: Int,
    val titleResId: Int,
    val format: ((Context, String) -> String)? = null,
) {
    SOURCE(R.drawable.ic_code, R.string.source_code),
    AUTHOR(R.drawable.ic_person, R.string.author_website),
    EMAIL(R.drawable.ic_email, R.string.author_email),
    LICENSE(
        R.drawable.ic_copyright,
        R.string.license,
        format = { context, text -> context.getString(R.string.license_FORMAT, text) },
    ),
    TRACKER(R.drawable.ic_bug_report, R.string.bug_tracker),
    CHANGELOG(R.drawable.ic_history, R.string.changelog),
    WEB(R.drawable.ic_public, R.string.project_website)
}
