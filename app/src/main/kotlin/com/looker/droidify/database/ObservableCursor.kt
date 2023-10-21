package com.looker.droidify.database

import android.database.ContentObservable
import android.database.ContentObserver
import android.database.Cursor
import android.database.CursorWrapper

class ObservableCursor(
    cursor: Cursor,
    private val observable: (
        register: Boolean,
        observer: () -> Unit
    ) -> Unit
) : CursorWrapper(cursor) {
    private var registered = false
    private val contentObservable = ContentObservable()

    private val onChange: () -> Unit = {
        contentObservable.dispatchChange(false, null)
    }

    init {
        observable(true, onChange)
        registered = true
    }

    override fun registerContentObserver(observer: ContentObserver) {
        super.registerContentObserver(observer)
        contentObservable.registerObserver(observer)
    }

    override fun unregisterContentObserver(observer: ContentObserver) {
        super.unregisterContentObserver(observer)
        contentObservable.unregisterObserver(observer)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun requery(): Boolean {
        if (!registered) {
            observable(true, onChange)
            registered = true
        }
        return super.requery()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun deactivate() {
        super.deactivate()
        deactivateOrClose()
    }

    override fun close() {
        super.close()
        contentObservable.unregisterAll()
        deactivateOrClose()
    }

    private fun deactivateOrClose() {
        observable(false, onChange)
        registered = false
    }
}
