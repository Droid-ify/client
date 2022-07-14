object Core {
	private const val coreVersion = "1.8.0"
	const val core = "androidx.core:core-ktx:$coreVersion"
}

object Coil {
	private const val coilVersion = "2.1.0"
	const val coil = "io.coil-kt:coil:$coilVersion"
}

object Coroutines {
	private const val coroutinesVersion = "1.6.3"
	const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
	const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
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

object AndroidX {

	const val appCompat = "androidx.appcompat:appcompat:1.4.2"

	private const val version = "1.5.0"
	const val fragment = "androidx.fragment:fragment-ktx:$version"
	const val activity = "androidx.activity:activity-ktx:$version"

	const val preference = "androidx.preference:preference-ktx:1.2.0"
	const val material = "com.google.android.material:material:1.6.1"
}

object OkHttp {
	private const val version = "5.0.0-alpha.9"
	const val okhttp = "com.squareup.okhttp3:okhttp:$version"
}

object RxJava {
	const val rxjava = "io.reactivex.rxjava3:rxjava:3.1.5"
	const val android = "io.reactivex.rxjava3:rxandroid:3.0.0"
}

object Others {
	const val libsu = "com.github.topjohnwu.libsu:core:3.2.1"
	const val fastScroller = "me.zhanghai.android.fastscroll:library:1.1.8"
	private const val shizukuVersion = "12.1.0"
	const val shizukuApi = "dev.rikka.shizuku:api:$shizukuVersion"
	const val shizukuProvider = "dev.rikka.shizuku:provider:$shizukuVersion"
}

object Jackson {
	const val core = "com.fasterxml.jackson.core:jackson-core:2.13.3"
}

object Gson {
	private const val version = "2.9.0"
	const val gson = "com.google.code.gson:gson:$version"
}

object Lifecycle {
	private const val lifecycleVersion = "2.5.0"
	const val runtime =
		"androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion"
}

object Room {
	private const val roomVersion = "2.4.2"
	const val roomRuntime = "androidx.room:room-runtime:$roomVersion"
	const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
	const val roomKtx = "androidx.room:room-ktx:$roomVersion"
}

object Version {
	const val kotlinVersion = "1.7.10"
}