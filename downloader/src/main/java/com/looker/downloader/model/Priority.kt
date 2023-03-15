package com.looker.downloader.model

sealed interface Priority : Comparable<Priority> {

	object LOW : Priority {
		override fun toString(): String {
			return "LOW"
		}

		override fun compareTo(other: Priority): Int {
			return when (other) {
				is HIGH -> -1
				is MEDIUM -> -1
				is LOW -> 0
			}
		}
	}

	object MEDIUM : Priority {
		override fun toString(): String {
			return "MEDIUM"
		}

		override fun compareTo(other: Priority): Int {
			return when (other) {
				is HIGH -> -1
				is MEDIUM -> 0
				is LOW -> 1
			}
		}
	}

	object HIGH : Priority {
		override fun toString(): String {
			return "HIGH"
		}

		override fun compareTo(other: Priority): Int {
			return when (other) {
				is HIGH -> 0
				is MEDIUM -> 1
				is LOW -> 1
			}
		}
	}
}