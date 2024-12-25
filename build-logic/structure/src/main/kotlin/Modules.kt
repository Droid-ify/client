import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project

object Modules {
    const val app = ":app"
    const val coreCommon = ":core:common"
    const val coreData = ":core:data"
    const val coreDatabase = ":core:database"
    const val coreDatastore = ":core:datastore"
    const val coreDI = ":core:di"
    const val coreDomain = ":core:domain"
    const val coreNetwork = ":core:network"
    const val installer = ":installer"
    const val sync = ":sync:fdroid"
}

fun DependencyHandlerScope.modules(vararg module: String) {
    module.forEach {
        add("implementation", project(it))
    }
}
