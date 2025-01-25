package com.looker.droidify.data.local.model

data class AppLinksEntity(
    val changelog: String?,
    val issueTracker: String?,
    val translation: String?,
    val sourceCode: String?,
    val id: Int = -1,
)
