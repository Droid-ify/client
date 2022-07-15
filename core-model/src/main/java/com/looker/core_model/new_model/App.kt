package com.looker.core_model.new_model

import com.looker.core_model.util.STRING_DELIMITER

data class App(
	val icon: String,
	val packageName: String,
	val license: String,
	val suggestedVersionName: String,
	val website: String,
	val sourceCode: String,
	val changelog: String,
	val issueTracker: String,
	val translation: String,
	val added: Long,
	val lastUpdated: Long,
	val suggestedVersionCode: Long,
	val author: Author,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val localized: List<Localized>,
	val donate: List<Donate>,
	val apks: List<Apk>
)

sealed class Donate(val id: String) {
	data class Regular(val url: String) : Donate(url)
	data class Bitcoin(val address: String) : Donate(address)
	data class LiteCoin(val address: String) : Donate(address)
	data class Flattr(val userId: String) : Donate(userId)
	data class LiberaPay(val userId: String) : Donate(userId)
	data class OpenCollective(val userId: String) : Donate(userId)
}

data class Author(
	val name: String,
	val website: String,
	val email: String
) {
	override fun toString(): String = name + STRING_DELIMITER + website + STRING_DELIMITER + email
}

fun String.toAuthor(): Author {
	val authorInList = split(STRING_DELIMITER)
	return Author(
		authorInList[0],
		authorInList[1],
		authorInList[2]
	)
}