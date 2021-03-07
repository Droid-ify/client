package com.looker.droidify.database

import android.content.Context
import android.database.Cursor
import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.loader.content.AsyncTaskLoader

class QueryLoader(context: Context, private val query: (CancellationSignal) -> Cursor?):
  AsyncTaskLoader<Cursor>(context) {
  private val observer = ForceLoadContentObserver()
  private var cancellationSignal: CancellationSignal? = null
  private var cursor: Cursor? = null

  override fun loadInBackground(): Cursor? {
    val cancellationSignal = synchronized(this) {
      if (isLoadInBackgroundCanceled) {
        throw OperationCanceledException()
      }
      val cancellationSignal = CancellationSignal()
      this.cancellationSignal = cancellationSignal
      cancellationSignal
    }
    try {
      val cursor = query(cancellationSignal)
      if (cursor != null) {
        try {
          cursor.count // Ensure the cursor window is filled
          cursor.registerContentObserver(observer)
        } catch (e: Exception) {
          cursor.close()
          throw e
        }
      }
      return cursor
    } finally {
      synchronized(this) {
        this.cancellationSignal = null
      }
    }
  }

  override fun cancelLoadInBackground() {
    super.cancelLoadInBackground()

    synchronized(this) {
      cancellationSignal?.cancel()
    }
  }

  override fun deliverResult(data: Cursor?) {
    if (isReset) {
      data?.close()
    } else {
      val oldCursor = cursor
      cursor = data
      if (isStarted) {
        super.deliverResult(data)
      }
      if (oldCursor != data) {
        oldCursor.closeIfNeeded()
      }
    }
  }

  override fun onStartLoading() {
    cursor?.let(this::deliverResult)
    if (takeContentChanged() || cursor == null) {
      forceLoad()
    }
  }

  override fun onStopLoading() {
    cancelLoad()
  }

  override fun onCanceled(data: Cursor?) {
    data.closeIfNeeded()
  }

  override fun onReset() {
    super.onReset()

    stopLoading()
    cursor.closeIfNeeded()
    cursor = null
  }

  private fun Cursor?.closeIfNeeded() {
    if (this != null && !isClosed) {
      close()
    }
  }
}
