package com.looker.droidify.data.local.converters

import androidx.room.TypeConverter
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.v2.model.PermissionV2
import kotlinx.serialization.builtins.ListSerializer

private val permissionListSerializer = ListSerializer(PermissionV2.serializer())

object PermissionConverter {

    @TypeConverter
    fun fromPermissionV2List(value: List<PermissionV2>): String {
        return JsonParser.encodeToString(permissionListSerializer, value)
    }

    @TypeConverter
    fun toPermissionV2List(value: String): List<PermissionV2> {
        return JsonParser.decodeFromString(permissionListSerializer, value)
    }
}
