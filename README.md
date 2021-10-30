<div align="center">

<img width="" src="src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Droid-ify" align="center">

# Droid-ify

Material-ify with Droid-ify.


[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="Get it on IzzyDroid"
width="48%">](https://android.izzysoft.de/repo/apk/com.looker.droidify)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
width="48%">](https://f-droid.org/packages/com.looker.droidify)
[<img src="https://support.crowdin.com/assets/logos/crowdin-logo-small-example@2x.png"
alt="Crowdin"
width="48%">](https://crowdin.com/project/droid-ify)
&nbsp;&nbsp;
[<img src="https://www.freepnglogos.com/uploads/telegram-logo-11.png"
alt="Join Telegram Channel"
width="48%">](https://t.me/droid_ify)

[![Github All Releases](https://img.shields.io/github/downloads/Iamlooker/Droid-ify/total.svg)]()

<div align="left">

## :book: Description

Unofficial F-Droid client with Material UI.

This app is an Direct Adaptation/Modification
of [Foxy-Droid](https://github.com/kitsunyan/foxy-droid/)

### :mag_right: Features

* Material F-Droid style
* No cards or inappropriate animations
* Fast repository syncing
* Standard Android components and minimal dependencies

### :camera: Screenshots

<img src="metadata/en-US/images/phoneScreenshots/home-light.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-light.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/home-dark.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-dark.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/home-amoled.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-amoled.png" width="48%" />

## :hammer: Building and Installing

Specify your Android SDK path either using the `ANDROID_HOME` environment variable, or by filling
out the `sdk.dir`
property in `local.properties`.

Signing can be done automatically using `keystore.properties` as follows:

```properties
store.file=/path/to/keystore
store.password=key-store-password
key.alias=key-alias
key.password=key-password
```

Run `./gradlew assembleRelease` to build the package, which can be installed using the Android
package manager.

## :scroll: License

Droid-ify is available under the terms of the GNU General Public License v3 or later. Copyright ©
2020 Iamlooker.
