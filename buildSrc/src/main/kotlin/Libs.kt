object AndroidX {
	const val appCompat = "androidx.appcompat:appcompat:1.6.1"

	const val desugar = "com.android.tools:desugar_jdk_libs:1.2.2"

	const val preference = "androidx.preference:preference-ktx:1.2.0"
	const val recyclerView = "androidx.recyclerview:recyclerview:1.3.0"
	const val material = "com.google.android.material:material:1.8.0"
}

object Core {
	private const val coreVersion = "1.10.0-rc01"
	const val core = "androidx.core:core-ktx:$coreVersion"
}

object Coil {
	private const val coilVersion = "2.2.2"
	const val coil = "io.coil-kt:coil:$coilVersion"
}

object Coroutines {
	private const val coroutinesVersion = "1.7.0-Beta"
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
		"/META-INF/versions/9/previous-**.bin",
		"/okhttp3/internal/publicsuffix/*"
	)
}

object Hilt {
	private const val version = "2.45"
	const val classpath = "com.google.dagger:hilt-android-gradle-plugin:$version"
	const val android = "com.google.dagger:hilt-android:$version"

	const val compiler = "com.google.dagger:hilt-compiler:$version"
	const val plugin = "dagger.hilt.android.plugin"
	private const val androidXHilt = "1.0.0"
	const val work = "androidx.hilt:hilt-work:$androidXHilt"
	const val androidX = "androidx.hilt:hilt-compiler:$androidXHilt"
}

object Jackson {
	const val core = "com.fasterxml.jackson.core:jackson-core:2.14.2"
}

object Kotlin {
	const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0"
	const val datetime = "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0"
}

object Ktor {
	private const val version = "2.2.4"
	const val core = "io.ktor:ktor-client-core:$version"
	const val okhttp = "io.ktor:ktor-client-okhttp:$version"
	const val logging = "io.ktor:ktor-client-logging:$version"
}

object Lifecycle {
	private const val lifecycleVersion = "2.6.0"
	const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion"
	const val livedata = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion"
	const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion"

	const val fragment = "androidx.fragment:fragment-ktx:1.5.5"
	const val activity = "androidx.activity:activity-ktx:1.7.0-rc01"
}

object Navigation {
	private const val version = "2.6.0"
	const val fragment = "androidx.navigation:navigation-fragment-ktx:$version"
	const val ui = "androidx.navigation:navigation-ui:$version"
	const val dynamicFeature = "androidx.navigation:navigation-dynamic-features-fragment:$version"
}

object OkHttp {
	private const val version = "5.0.0-alpha.10"
	const val okhttp = "com.squareup.okhttp3:okhttp:$version"
}

object Others {
	const val libsu = "com.github.topjohnwu.libsu:core:5.0.4"
	const val zoomage = "com.jsibbold:zoomage:1.3.1"
	private const val shizukuVersion = "13.0.0"
	const val shizukuApi = "dev.rikka.shizuku:api:$shizukuVersion"
	const val shizukuProvider = "dev.rikka.shizuku:provider:$shizukuVersion"
}

object Room {
	private const val roomVersion = "2.5.0"
	const val runtime = "androidx.room:room-runtime:$roomVersion"
	const val compiler = "androidx.room:room-compiler:$roomVersion"
	const val ktx = "androidx.room:room-ktx:$roomVersion"
}

object SQLite {
	private const val version = "2.3.0"
	const val ktx = "androidx.sqlite:sqlite-ktx:$version"
}

object Test {
	const val jUnitRunner = "androidx.test.runner.AndroidJUnitRunner"
	const val jUnit = "junit:junit:4.13.2"
	const val androidJUnit = "androidx.test.ext:junit:1.1.3"
	const val espresso = "androidx.test.espresso:espresso-core:3.4.0"
}

object Version {
	const val kotlin = "1.8.20"
	const val ksp = "1.8.20-1.0.10"
}

object Work {
	private const val version = "2.7.1"
	const val manager = "androidx.work:work-runtime-ktx:$version"
}
