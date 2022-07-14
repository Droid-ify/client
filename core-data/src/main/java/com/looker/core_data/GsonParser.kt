package com.looker.core_data

import com.google.gson.Gson
import java.io.Reader
import java.lang.reflect.Type

class GsonParser(private val gson: Gson) : JsonParser {

	override fun <T> fromJson(json: Reader, type: Type): T? = gson.fromJson(json, type)

	override fun <T> toJson(obj: T, type: Type): String? = gson.toJson(obj, type)

}