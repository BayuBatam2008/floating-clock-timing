plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

subprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.apache.commons:commons-net"))
                .using(module("commons-net:commons-net:3.9.0"))
        }
    }
}
