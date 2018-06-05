Contributing
============

**Note:** This project is in the [public domain](UNLICENSE). If you contribute any [non-trivial][15]
patches or translations the following applies:

    I dedicate any and all copyright interest in this software to the
    public domain. I make this dedication for the benefit of the public at
    large and to the detriment of my heirs and successors. I intend this
    dedication to be an overt act of relinquishment in perpetuity of all
    present and future rights to this software under copyright law.

#### Would you like to contribute code?

1. [Fork SeriesGuide][11]. See further setup instructions below.
2. Create a new branch ([using GitHub][14] or the command `git checkout -b descriptive-branch-name dev`) and make [great commits + messages][10].
3. [Start a pull request][6]. Reference [existing issues][7] when possible.

#### No code!
* You can [get help][12].
* You can [suggest features][9].
* You can [discuss a bug][7] or if it was not reported yet [submit a bug][8].
* You can [translate strings][4].

Repository structure
--------------------

- `dev`, the main development and [test release][2] branch.
- `master`, the stable release branch. Always the latest [stable version][1] of SeriesGuide.

Setup
-----

This project is built with Gradle, the [Android Gradle plugin][3] and uses jar and Maven dependencies.

1. Clone this repository inside your working folder. I suggest only cloning the latest revision, like `git clone --depth=1 https://github.com/UweTrottmann/SeriesGuide.git`.
2. Create the `gradle.properties` and `fabric.properties` files as noted below.
3. Android Studio: import the `settings.gradle` file.

Before your first build create `gradle.properties` in the root directory (where `settings.gradle` is) and add the following values. They do not need to be valid if you do not plan to use that functionality:

```
# Credentials to publish the API jar
ossrhUsername=<your sonatype username>
ossrhPassword=<your sonatype password>

# API keys for integrated services
TMDB_API_KEY=<your api key>
TRAKT_CLIENT_ID=<your trakt client id>
TRAKT_CLIENT_SECRET=<your trakt client secret>
TVDB_API_KEY=<your api key>

# Play Store in-app billing public key
IAP_KEY_A=dummy
IAP_KEY_B=dummy
IAP_KEY_C=dummy
IAP_KEY_D=dummy
```

Also create `SeriesGuide/fabric.properties` for [Crashlytics][13]. You may use the dummy values below:

```
# crashlytics dummy values
apiSecret=0000000000000000000000000000000000000000000000000000000000000000
apiKey=0
```

Now build any variant of the **pure flavor**, for developing probably `pureDebug` (flavor + build type, see [instructions about product flavors][5]) defined in `SeriesGuide/build.gradle`.

 [1]: https://seriesgui.de
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: https://developer.android.com/studio/build/index.html
 [4]: https://crowdin.com/project/seriesguide-translations
 [5]: https://developer.android.com/studio/build/build-variants.html#product-flavors
 [6]: https://github.com/UweTrottmann/SeriesGuide/compare
 [7]: https://github.com/UweTrottmann/SeriesGuide/issues
 [8]: https://github.com/UweTrottmann/SeriesGuide/issues/new
 [9]: https://seriesguide.uservoice.com
 [10]: http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message
 [11]: https://github.com/UweTrottmann/SeriesGuide/fork
 [12]: https://seriesgui.de/help
 [13]: https://get.fabric.io/crashlytics
 [14]: https://help.github.com/articles/creating-and-deleting-branches-within-your-repository/
 [15]: http://www.gnu.org/prep/maintain/maintain.html#Legally-Significant
 