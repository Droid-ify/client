package com.looker.droidify.entity

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.droidify.utility.extension.json.forEachKey

data class ProductPreference(val ignoreUpdates: Boolean, val ignoreVersionCode: Long) {
	fun shouldIgnoreUpdate(versionCode: Long): Boolean {
		return ignoreUpdates || ignoreVersionCode == versionCode
	}

	fun serialize(generator: JsonGenerator) {
		generator.writeBooleanField("ignoreUpdates", ignoreUpdates)
		generator.writeNumberField("ignoreVersionCode", ignoreVersionCode)
	}

	companion object {
		fun deserialize(parser: JsonParser): ProductPreference {
			var ignoreUpdates = false
			var ignoreVersionCode = 0L
			parser.forEachKey {
				when {
					it.boolean("ignoreUpdates") -> ignoreUpdates = valueAsBoolean
					it.number("ignoreVersionCode") -> ignoreVersionCode = valueAsLong
					else -> skipChildren()
				}
			}
			return ProductPreference(ignoreUpdates, ignoreVersionCode)
		}
	}
}
