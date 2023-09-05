package com.looker.droidify.ui.app_detail

import androidx.lifecycle.ViewModel
import com.looker.core.common.extension.asStateFlow
import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.database.Database
import com.looker.installer.InstallManager
import com.looker.installer.model.InstallerQueueState
import com.looker.installer.model.contains
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
	installer: InstallManager
) : ViewModel() {

	private var _packageName: String? = null
	val packageName: String get() = _packageName!!

	fun setPackageName(name: String) {
		_packageName = name
	}

	val installerState = installer
		.getStatus()
		.filter { packageName in it }
		.asStateFlow(InstallerQueueState.EMPTY)

	val state by  lazy {
		combine(
			Database.ProductAdapter.getStream(packageName),
			Database.RepositoryAdapter.getAllStream(),
			Database.InstalledAdapter.getStream(packageName),
		) { products, repositories, installedItem ->
			val idAndRepos = repositories.associateBy { it.id }
			val filteredProducts = products.filter { product ->
				idAndRepos[product.repositoryId] != null
			}
			AppDetailUiState(
				products = filteredProducts,
				repos = repositories,
				installedItem = installedItem,
				isSelf = packageName == BuildConfig.APPLICATION_ID
			)
		}.asStateFlow(AppDetailUiState())
	}

	override fun onCleared() {
		super.onCleared()
		_packageName = null
	}
}

data class AppDetailUiState(
	val products: List<Product> = emptyList(),
	val repos: List<Repository> = emptyList(),
	val installedItem: InstalledItem? = null,
	val isSelf: Boolean = false,
	val isFavourite: Boolean = false,
	val allowIncompatibleVersions: Boolean = false
)