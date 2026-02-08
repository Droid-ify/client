package com.looker.droidify.database

import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.model.ProductItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class CursorOwner : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    sealed class Request(val id: Int) {

        data class Available(
            val searchQuery: String,
            val section: ProductItem.Section,
            val order: SortOrder,
        ) : Request(1)

        data class Installed(
            val searchQuery: String,
            val order: SortOrder,
        ) : Request(2)

        data class Updates(
            val searchQuery: String,
            val order: SortOrder,
        ) : Request(3)

        data object Repositories : Request(4)
    }

    interface Callback {
        fun onCursorData(request: Request, cursor: Cursor?)
    }

    private data class ActiveRequest(
        val request: Request,
        val callback: Callback?,
        val cursor: Cursor?,
    )

    private val activeRequests = mutableMapOf<Int, ActiveRequest>()

    fun attach(callback: Callback, request: Request) {
        val oldActiveRequest = activeRequests[request.id]
        if (oldActiveRequest?.callback != null &&
            oldActiveRequest.callback != callback && oldActiveRequest.cursor != null
        ) {
            oldActiveRequest.callback.onCursorData(oldActiveRequest.request, null)
        }
        val cursor = if (oldActiveRequest?.request == request && oldActiveRequest.cursor != null) {
            callback.onCursorData(request, oldActiveRequest.cursor)
            oldActiveRequest.cursor
        } else {
            null
        }
        activeRequests[request.id] = ActiveRequest(request, callback, cursor)
        if (cursor == null) {
            LoaderManager.getInstance(this).restartLoader(request.id, null, this)
        }
    }

    fun detach(callback: Callback) {
        for (id in activeRequests.keys) {
            val activeRequest = activeRequests[id]!!
            if (activeRequest.callback == callback) {
                activeRequests[id] = activeRequest.copy(callback = null)
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val request = activeRequests[id]!!.request
        return QueryLoader(requireContext()) {
            val settings = runBlocking { settingsRepository.getInitial() }
            when (request) {
                is Request.Available ->
                    Database.ProductAdapter
                        .query(
                            installed = false,
                            updates = false,
                            searchQuery = request.searchQuery,
                            section = request.section,
                            order = request.order,
                            signal = it,
                            skipSignatureCheck = settings.ignoreSignature,
                        )

                is Request.Installed ->
                    Database.ProductAdapter
                        .query(
                            installed = true,
                            updates = false,
                            searchQuery = request.searchQuery,
                            section = ProductItem.Section.All,
                            order = request.order,
                            signal = it,
                            skipSignatureCheck = settings.ignoreSignature,
                        )

                is Request.Updates ->
                    Database.ProductAdapter
                        .query(
                            installed = true,
                            updates = true,
                            searchQuery = request.searchQuery,
                            section = ProductItem.Section.All,
                            order = request.order,
                            signal = it,
                            skipSignatureCheck = settings.ignoreSignature,
                        )

                is Request.Repositories -> Database.RepositoryAdapter.query(it)
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        val activeRequest = activeRequests[loader.id]
        if (activeRequest != null) {
            activeRequests[loader.id] = activeRequest.copy(cursor = data)
            activeRequest.callback?.onCursorData(activeRequest.request, data)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) = onLoadFinished(loader, null)
}
