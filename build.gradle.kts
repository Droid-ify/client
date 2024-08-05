plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.android.library) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.ktlint) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.hilt) apply false
	alias(libs.plugins.kotlin.serialization) apply false
	alias(libs.plugins.kotlin.parcelize) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            // re-enable those
            failOnVersionConflict()

            force(libs.androidx.activity)
            force(libs.androidx.activity.ktx)
            force(libs.androidx.annotation)
            force(libs.androidx.annotation.experimental)
            force(libs.androidx.appcompat)
            force(libs.androidx.appcompat.resources)
            force(libs.androidx.arch.core.common)
            force(libs.androidx.arch.core.runtime)
            force(libs.androidx.concurrent.futures)
            force(libs.androidx.collection)
            force(libs.androidx.collection.ktx)
            force(libs.androidx.core)
            force(libs.androidx.core.ktx)
            force(libs.androidx.customview)
            force(libs.androidx.drawerlayout)
            force(libs.androidx.emoji2)
            force(libs.androidx.emoji2.views.helper)
            force(libs.androidx.fragment)
            force(libs.androidx.fragment.ktx)
            force(libs.androidx.lifecycle.common)
            force(libs.androidx.lifecycle.livedata)
            force(libs.androidx.lifecycle.livedata.ktx)
            force(libs.androidx.lifecycle.livedata.core)
            force(libs.androidx.lifecycle.livedata.core.ktx)
            force(libs.androidx.lifecycle.process)
            force(libs.androidx.lifecycle.runtime)
            force(libs.androidx.lifecycle.runtime.ktx)
            force(libs.androidx.lifecycle.service)
            force(libs.androidx.lifecycle.viewModel)
            force(libs.androidx.lifecycle.viewModel.ktx)
            force(libs.androidx.lifecycle.viewModel.savedstate)
            force(libs.androidx.recyclerview)
            force(libs.androidx.room)
            force(libs.androidx.room.ktx)
            force(libs.androidx.savedstate)
            force(libs.androidx.sqlite)
            force(libs.androidx.sqlite.framework)
            force(libs.androidx.sqlite.ktx)
            force(libs.androidx.startup.runtime)
            force(libs.androidx.transition)
            force(libs.androidx.viewpager2)
            force(libs.androidx.work.runtime)
            force(libs.androidx.work.runtime.ktx)
            force(libs.dagger.hilt.core)
            force(libs.errorprone.annotations)
            force(libs.guava)
            force(libs.guava.listenablefuture)
            force(libs.hilt.android)
            force(libs.jetbrains.annotations)
            force(libs.kotlin.parcelize.runtime)
            force(libs.kotlin.reflect)
            force(libs.kotlin.stdlib)
            force(libs.kotlin.stdlib.jdk7)
            force(libs.kotlin.stdlib.jdk8)
            force(libs.kotlin.stdlib.common)
            force(libs.kotlin.test)
            force(libs.kotlin.test.junit)
            force(libs.kotlinPoet)
            force(libs.kotlinx.coroutines.android)
            force(libs.kotlinx.coroutines.core)
            force(libs.kotlinx.coroutines.jdk8)
            force(libs.kotlinx.coroutines.slf4j)
            force(libs.ksp.symbolProcessingApi)
            force(libs.okio)
            force(libs.slf4j.api)
        }
    }
}
