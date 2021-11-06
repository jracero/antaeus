plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation("com.google.code.gson:gson:2.8.5");
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.8.+")
    implementation("joda-time:joda-time:2.10.2")
}
