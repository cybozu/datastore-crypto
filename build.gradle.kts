// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint.gradle) apply false
    alias(libs.plugins.licensee) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.maven.publish) apply false
}

fun registerCompositeTask(taskName: String, taskGroup: String, taskDescription: String) {
    tasks.register(taskName) {
        group = taskGroup
        description = taskDescription

        val buildLogic = gradle.includedBuild("build-logic")
        val settingsFile = buildLogic
            .projectDir
            .resolve("settings.gradle.kts")
        val buildLogicProjects = settingsFile
            .readLines()
            .filter { it.trim().startsWith("include(") }
            .map { line ->
                line.substringAfter("include(").substringBefore(")").trim('"', '\'')
            }

        buildLogicProjects.forEach { projectPath ->
            dependsOn(buildLogic.task("$projectPath:$taskName"))
        }

        dependsOn(subprojects.map { it.tasks.matching { task -> task.name == taskName } })
    }
}

registerCompositeTask(
    taskName = "ktlintFormat",
    taskGroup = "formatting",
    taskDescription = "Run ktlintFormat on all projects including build-logic"
)

registerCompositeTask(
    taskName = "ktlintCheck",
    taskGroup = "verification",
    taskDescription = "Run ktlintCheck on all projects including build-logic"
)
