package com.looker.droidify.data.local.model

/**
 * Stable integer discriminators for the `type` columns of the `graphic`,
 * `screenshot` and `donate` tables. Never reorder — values are persisted.
 */
enum class GraphicType(val value: Int) {
    FEATURE_GRAPHIC(0),
    PROMO_GRAPHIC(1),
    TV_BANNER(2),
    VIDEO(3);

    companion object {
        fun fromValue(value: Int): GraphicType = entries.first { it.value == value }
    }
}

enum class ScreenshotType(val value: Int) {
    PHONE(0),
    SEVEN_INCH(1),
    TEN_INCH(2),
    WEAR(3),
    TV(4);

    companion object {
        fun fromValue(value: Int): ScreenshotType = entries.first { it.value == value }
    }
}

enum class DonateType(val value: Int) {
    REGULAR(0),
    BITCOIN(1),
    LITECOIN(2),
    LIBERAPAY(3),
    OPEN_COLLECTIVE(4);

    companion object {
        fun fromValue(value: Int): DonateType = entries.first { it.value == value }
    }
}
