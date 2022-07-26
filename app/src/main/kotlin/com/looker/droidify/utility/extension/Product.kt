package com.looker.droidify.utility.extension

import com.looker.core_common.R.string as stringRes

enum class Order(val titleResId: Int) {
	NAME(stringRes.name),
	DATE_ADDED(stringRes.whats_new),
	LAST_UPDATE(stringRes.recently_updated)
}