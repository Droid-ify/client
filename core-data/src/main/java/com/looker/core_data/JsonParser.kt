package com.looker.core_data

import java.io.Reader
import java.lang.reflect.Type

interface JsonParser {

	fun <T> fromJson(json: Reader, type: Type): T?

	fun <T> toJson(obj: T, type: Type): String?

}