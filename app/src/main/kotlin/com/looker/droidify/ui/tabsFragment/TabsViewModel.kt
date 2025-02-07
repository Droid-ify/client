package com.looker.droidify.ui.tabsFragment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.data.local.dao.AppDao
import com.looker.droidify.data.local.dao.RepoDao
import com.looker.droidify.data.local.model.RepoEntity
import com.looker.droidify.data.local.model.toRepo
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.model.ProductItem
import com.looker.droidify.sync.Syncable
import com.looker.droidify.sync.v2.model.IndexV2
import com.looker.droidify.ui.tabsFragment.TabsFragment.BackAction
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repoDao: RepoDao,
    private val appDao: AppDao,
    private val syncable: Syncable<IndexV2>,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val currentSection =
        savedStateHandle.getStateFlow<ProductItem.Section>(STATE_SECTION, ProductItem.Section.All)

    val sortOrder = settingsRepository
        .get { sortOrder }
        .asStateFlow(SortOrder.UPDATED)

    val allowHomeScreenSwiping = settingsRepository
        .get { homeScreenSwiping }
        .asStateFlow(false)

    val sections =
        combine(
            Database.CategoryAdapter.getAllStream(),
            Database.RepositoryAdapter.getEnabledStream(),
        ) { categories, repos ->
            val productCategories = categories
                .asSequence()
                .sorted()
                .map(ProductItem.Section::Category)
                .toList()

            val enabledRepositories = repos
                .map { ProductItem.Section.Repository(it.id, it.name) }
            enabledRepositories.ifEmpty { setSection(ProductItem.Section.All) }
            listOf(ProductItem.Section.All) + productCategories + enabledRepositories
        }
            .catch { it.printStackTrace() }
            .asStateFlow(emptyList())

    val isSearchActionItemExpanded = MutableStateFlow(false)

    val showSections = MutableStateFlow(false)

    val backAction = combine(
        currentSection,
        isSearchActionItemExpanded,
        showSections,
        ::calcBackAction,
    ).asStateFlow(BackAction.None)

    fun setSection(section: ProductItem.Section) {
        savedStateHandle[STATE_SECTION] = section
    }

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setSortOrder(sortOrder)
        }
    }

    fun sync() {
        viewModelScope.launch {
            val repo = RepoEntity(
                id = 1,
                address = "https://apt.izzysoft.de/fdroid/repo",
                name = mapOf("en-US" to "IzzyOnDroid F-Droid Repo"),
                description = emptyMap(),
                fingerprint = Fingerprint("3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"),
                username = "",
                password = "",
                timestamp = 0L,
                icon = emptyMap(),
            )
            repoDao.upsert(repo)
            val (_, index) = syncable.sync(repo.toRepo("en-US", emptyList(), true))
            requireNotNull(index)
            val id = repoDao.upsertRepo(
                fingerprint = repo.fingerprint,
                repo = index.repo,
                username = repo.username,
                password = repo.password,
                id = repo.id,
            )
            appDao.upsertMetadata(
                repoId = id,
                metadatas = index.packages,
            )
        }
    }

    private fun calcBackAction(
        currentSection: ProductItem.Section,
        isSearchActionItemExpanded: Boolean,
        showSections: Boolean,
    ): BackAction {
        return when {
            currentSection != ProductItem.Section.All -> {
                BackAction.ProductAll
            }

            isSearchActionItemExpanded -> {
                BackAction.CollapseSearchView
            }

            showSections -> {
                BackAction.HideSections
            }

            else -> {
                BackAction.None
            }
        }
    }

    companion object {
        private const val STATE_SECTION = "section"
    }
}
