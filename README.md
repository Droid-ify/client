<div align="center">

<img width="" src="src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Droid-ify" align="center">

# Droid-ify

A quick material F-Droid client.

[![Github repo stars](https://img.shields.io/github/stars/Iamlooker/Droid-ify?style=flat)](https://github.com/Iamlooker/Droid-ify/stargazers)
[![Github License](https://img.shields.io/github/license/Iamlooker/Droid-ify)](https://github.com/Iamlooker/Droid-ify/blob/master/COPYING)
[![Github All Releases](https://img.shields.io/github/downloads/Iamlooker/Droid-ify/total.svg)](https://github.com/Iamlooker/Droid-ify/releases/)
[![Github release](https://img.shields.io/github/v/release/Iamlooker/Droid-ify?display_name=tag)](https://github.com/Iamlooker/Droid-ify/releases/latest)
[![Github watchers](https://img.shields.io/github/watchers/Iamlooker/Droid-ify?label=Watch)](https://github.com/Iamlooker/Droid-ify/watchers)
[![Github forks](https://img.shields.io/github/forks/Iamlooker/Droid-ify?label=Fork)](https://github.com/Iamlooker/Droid-ify/network/members)

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="Get it on IzzyDroid"
width="48%">](https://android.izzysoft.de/repo/apk/com.looker.droidify)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
width="48%">](https://f-droid.org/packages/com.looker.droidify)
[<img src="https://github.com/WeblateOrg/weblate/blob/main/weblate/static/weblate.svg"
alt="Weblate"
width="40%">](https://hosted.weblate.org/projects/droidify/localization/)
&nbsp;&nbsp;
[<img src="https://upload.wikimedia.org/wikipedia/commons/8/82/Telegram_logo.svg"
alt="Join Telegram Channel"
width="30%">](https://t.me/droid_ify)

<div align="left">

## :book: Features

* Material F-Droid style
* No cards or inappropriate animations
* Fast repository syncing
* Standard Android components and minimal dependencies

### :camera: Screenshots

<img src="metadata/en-US/images/phoneScreenshots/home-light.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-light.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/home-dark.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-dark.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/home-amoled.png" width="48%" /><img src="metadata/en-US/images/phoneScreenshots/app-amoled.png" width="48%" />

## :hammer: Building and Installing

Specify your Android SDK path either using the `ANDROID_HOME` environment variable, \
or by filling out the `sdk.dir` property in `local.properties`.

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

Licensed GPLv3+. \
Direct Adaptation/Modification of [Foxy-Droid](https://github.com/kitsunyan/foxy-droid/) \
Copyright © 2020–2021 Iamlooker and contributors.
