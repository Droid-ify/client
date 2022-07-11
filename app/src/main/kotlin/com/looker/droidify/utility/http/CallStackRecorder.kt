package com.looker.droidify.utility.http

const val OKHTTP_STACK_RECORDER_PROPERTY = "ru.gildor.coroutines.okhttp.stackrecorder"

const val OKHTTP_STACK_RECORDER_ON = "on"

const val OKHTTP_STACK_RECORDER_OFF = "off"

@JvmField
val isRecordStack = when (System.getProperty(OKHTTP_STACK_RECORDER_PROPERTY)) {
	OKHTTP_STACK_RECORDER_ON -> true
	OKHTTP_STACK_RECORDER_OFF, null, "" -> false
	else -> error("System property '$OKHTTP_STACK_RECORDER_PROPERTY' has unrecognized value '${System.getProperty(OKHTTP_STACK_RECORDER_PROPERTY)}'")
}