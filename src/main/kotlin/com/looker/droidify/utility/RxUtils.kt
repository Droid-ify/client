package com.looker.droidify.utility

import android.os.CancellationSignal
import android.os.OperationCanceledException
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import okhttp3.Call
import okhttp3.Response

object RxUtils {
    private class ManagedDisposable(private val cancel: () -> Unit) : Disposable {
        @Volatile
        var disposed = false
        override fun isDisposed(): Boolean = disposed

        override fun dispose() {
            disposed = true
            cancel()
        }
    }

    private fun <T, R> managedSingle(
        create: () -> T,
        cancel: (T) -> Unit,
        execute: (T) -> R
    ): Single<R> {
        return Single.create {
            val task = create()
            val thread = Thread.currentThread()
            val disposable = ManagedDisposable {
                thread.interrupt()
                cancel(task)
            }
            it.setDisposable(disposable)
            if (!disposable.isDisposed) {
                val result = try {
                    execute(task)
                } catch (e: Throwable) {
                    Exceptions.throwIfFatal(e)
                    if (!disposable.isDisposed) {
                        try {
                            it.onError(e)
                        } catch (inner: Throwable) {
                            Exceptions.throwIfFatal(inner)
                            RxJavaPlugins.onError(CompositeException(e, inner))
                        }
                    }
                    null
                }
                if (result != null && !disposable.isDisposed) {
                    it.onSuccess(result)
                }
            }
        }
    }

    fun <R> managedSingle(execute: () -> R): Single<R> {
        return managedSingle({ }, { }, { execute() })
    }

    fun callSingle(create: () -> Call): Single<Response> {
        return managedSingle(create, Call::cancel, Call::execute)
    }

    fun <T> querySingle(query: (CancellationSignal) -> T): Single<T> {
        return Single.create {
            val cancellationSignal = CancellationSignal()
            it.setCancellable {
                try {
                    cancellationSignal.cancel()
                } catch (e: OperationCanceledException) {
                    // Do nothing
                }
            }
            val result = try {
                query(cancellationSignal)
            } catch (e: OperationCanceledException) {
                null
            }
            if (result != null) {
                it.onSuccess(result)
            }
        }
    }
}
