package com.looker.droidify.datastore.model

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class CustomButton(
    val id: String,
    val label: String,
    val urlTemplate: String,
    val icon: CustomButtonIcon = CustomButtonIcon.LINK,
) {
    fun resolveUrl(
        packageName: String,
        appName: String,
        authorName: String,
    ): String {
        val encodedAppName =  Uri.encode(appName)
        val encodedAuthorName = Uri.encode(authorName)

        return urlTemplate
            .replace("{{package_name}}", packageName)
            .replace("{{ package_name }}", packageName)
            .replace("{{app_name}}", encodedAppName)
            .replace("{{ app_name }}", encodedAppName)
            .replace("{{author_name}}", encodedAuthorName)
            .replace("{{ author_name }}", encodedAuthorName)
    }

    companion object {
        val TEMPLATES = listOf(
            CustomButton(
                id = "exodus",
                label = "Exodus Privacy",
                urlTemplate = "https://reports.exodus-privacy.eu.org/en/reports/{{package_name}}/latest/",
                icon = CustomButtonIcon.PRIVACY,
            ),
            CustomButton(
                id = "plexus",
                label = "Plexus",
                urlTemplate = "https://plexus.techlore.tech/apps?q={{app_name}}",
                icon = CustomButtonIcon.PRIVACY,
            ),
            CustomButton(
                id = "playstore",
                label = "Play Store",
                urlTemplate = "https://play.google.com/store/apps/details?id={{package_name}}",
                icon = CustomButtonIcon.STORE,
            ),
            CustomButton(
                id = "alternativeto",
                label = "AlternativeTo",
                urlTemplate = "https://alternativeto.net/browse/search/?q={{app_name}}",
                icon = CustomButtonIcon.SEARCH,
            ),
            CustomButton(
                id = "apkmirror",
                label = "APKMirror",
                urlTemplate = "https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s={{package_name}}",
                icon = CustomButtonIcon.SEARCH,
            ),
        )
    }
}

@Serializable
enum class CustomButtonIcon {
    LINK,
    SEARCH,
    PRIVACY,
    STORE,
    CODE,
    DOWNLOAD,
    SHARE,
    BUG,
    INFO,
    EMAIL,
    PERSON,
    HISTORY,
    SETTINGS,
    TEXT_ONLY,
}
