object AndroidX {
	const val appCompat = "androidx.appcompat:appcompat:1.5.0"

	const val desugar = "com.android.tools:desugar_jdk_libs:1.1.5"

	const val preference = "androidx.preference:preference-ktx:1.2.0"
	const val material = "com.google.android.material:material:1.6.1"
}

object Core {
	private const val coreVersion = "1.8.0"
	const val core = "androidx.core:core-ktx:$coreVersion"
}

object Coil {
	private const val coilVersion = "2.2.0"
	const val coil = "io.coil-kt:coil:$coilVersion"
}

object Coroutines {
	private const val coroutinesVersion = "1.6.4"
	const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
	const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
}

object Datastore {
	private const val version = "1.0.0"
	const val datastore = "androidx.datastore:datastore-preferences:$version"
}

object Excludes {
	const val jniExclude = "/okhttp3/internal/publicsuffix/*"
	val listExclude: List<String> = listOf(
		"/DebugProbesKt.bin",
		"/kotlin/**.kotlin_builtins",
		"/kotlin/**.kotlin_metadata",
		"/META-INF/**.kotlin_module",
		"/META-INF/**.pro",
		"/META-INF/**.version",
		"/okhttp3/internal/publicsuffix/*"
	)
}

object Hilt {
	private const val version = "2.43.2"
	const val classpath = "com.google.dagger:hilt-android-gradle-plugin:$version"
	const val android = "com.google.dagger:hilt-android:$version"

	const val compiler = "com.google.dagger:hilt-compiler:$version"
	const val plugin = "dagger.hilt.android.plugin"
	private const val androidXHilt = "1.0.0"
	const val work = "androidx.hilt:hilt-work:$androidXHilt"
	const val androidX = "androidx.hilt:hilt-compiler:$androidXHilt"
	const val navigation = "androidx.hilt:hilt-navigation-compose:$androidXHilt"
}

object Jackson {
	const val core = "com.fasterxml.jackson.core:jackson-core:2.13.3"
}

object Kotlin {
	const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0"
	const val datetime = "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0"
}

object Ktor {
	private const val ktorVersion = "2.1.0"
	const val android = "io.ktor:ktor-client-android:$ktorVersion"
	const val json = "io.ktor:ktor-client-json:$ktorVersion"
	const val serialization = "io.ktor:ktor-client-serialization-jvm:$ktorVersion"
	const val logging = "io.ktor:ktor-client-logging-jvm:$ktorVersion"
}

object Lifecycle {
	private const val lifecycleVersion = "2.5.1"
	const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion"
	const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion"

	private const val version = "1.5.1"
	const val fragment = "androidx.fragment:fragment-ktx:$version"
	const val activity = "androidx.activity:activity-ktx:$version"
}

object OkHttp {
	private const val version = "5.0.0-alpha.10"
	const val okhttp = "com.squareup.okhttp3:okhttp:$version"
}

object Others {
	const val libsu = "com.github.topjohnwu.libsu:core:3.2.1"
	const val fastScroller = "me.zhanghai.android.fastscroll:library:1.1.8"
	private const val shizukuVersion = "12.1.0"
	const val shizukuApi = "dev.rikka.shizuku:api:$shizukuVersion"
	const val shizukuProvider = "dev.rikka.shizuku:provider:$shizukuVersion"
}

object Room {
	private const val roomVersion = "2.4.3"
	const val roomRuntime = "androidx.room:room-runtime:$roomVersion"
	const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
	const val roomKtx = "androidx.room:room-ktx:$roomVersion"
}

object RxJava {
	const val rxjava = "io.reactivex.rxjava3:rxjava:3.1.5"
	const val android = "io.reactivex.rxjava3:rxandroid:3.0.0"
}

object Startup {
	private const val startupVersion = "1.1.1"
	const val lib = "androidx.startup:startup-runtime:$startupVersion"
}

object Version {
	const val kotlin = "1.7.10"
	const val ksp = "1.7.10-1.0.6"
}

object Work {
	private const val version = "2.7.1"
	const val manager = "androidx.work:work-runtime-ktx:$version"
}
