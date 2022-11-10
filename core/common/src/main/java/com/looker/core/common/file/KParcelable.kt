package com.looker.core.common.file

import android.os.Parcel
import android.os.Parcelable

interface KParcelable : Parcelable {
	override fun describeContents(): Int = 0
	override fun writeToParcel(dest: Parcel, flags: Int) = Unit

	companion object {
		inline fun <reified T> creator(crossinline create: (source: Parcel) -> T): Parcelable.Creator<T> {
			return object : Parcelable.Creator<T> {
				override fun createFromParcel(source: Parcel): T = create(source)
				override fun newArray(size: Int): Array<T?> = arrayOfNulls(size)
			}
		}
	}
}