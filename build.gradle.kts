plugins {
    java
    alias(libs.plugins.pluginYmlPaper)
}

group = "uk.co.notnull"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
	maven {
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
	mavenLocal()
}

dependencies {
	compileOnly(libs.paperApi)
}

paper {
    main = "uk.co.notnull.messagecommands.MessageCommands"
    apiVersion = libs.versions.paperApi.get().replace(".build.+", "")
    authors = listOf("Jim (AnEnragedPigeon)")
    description = "Simple plugin for configurable canned message commands"
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        options.encoding = "UTF-8"
    }
}
