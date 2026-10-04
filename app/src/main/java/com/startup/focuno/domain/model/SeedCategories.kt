package com.startup.focuno.domain.model

/** Built-in starting categories. A user override stored in the database always wins over these. */
object SeedCategories {

    val distracting: Set<String> = setOf(
        "com.instagram.android",
        "com.instagram.lite",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "in.mohalla.sharechat",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.pinterest",
        "com.netflix.mediaclient",
        "in.mohalla.video",
        "com.eterno",
    )

    // Deliberately short: only apps whose package ids are well known. Users can mark any other app as productive.
    val productive: Set<String> = setOf(
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.classroom",
        "com.ichi2.anki",
        "notion.id",
        "com.duolingo",
        "org.khanacademy.android",
        "xyz.penpencil.physicswala",
        "org.coursera.android",
        "com.amazon.kindle",
        "com.google.android.keep",
        "com.google.android.calendar",
        "com.google.android.calculator",
        "com.android.calculator2",
    )

    fun categoryOf(packageName: String): AppCategory = when (packageName) {
        in distracting -> AppCategory.DISTRACTING
        in productive -> AppCategory.PRODUCTIVE
        else -> AppCategory.NEUTRAL
    }
}
