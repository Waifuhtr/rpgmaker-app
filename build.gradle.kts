plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    // Kotlin 2.x'te Compose derleyicisi ayrı bir plugin olarak gelir; sürümü Kotlin ile aynı olmalı.
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
}
