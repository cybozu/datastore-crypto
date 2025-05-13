import com.cybozu.datastore.crypto.buildlogic.convention.getPluginId
import com.cybozu.datastore.crypto.buildlogic.convention.libs
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class MavenPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.getPluginId("maven-publish"))

            publishing {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/cybozu-private/cybozux.authentication")
                    }
                }
            }

            // Check the presence of credentials just before executing the publish task
            // Otherwise, even people who do not need to run the publish task will not be able to run any Gradle tasks unless they set up credentials.
            configureGithubCredentialsLazily()
        }
    }

    private fun Project.configureGithubCredentialsLazily() {
        val localProperties = loadLocalProperties(rootProject)

        tasks.withType<PublishToMavenRepository>().configureEach {
            doFirst {
                publishing {
                    val githubUsername = System.getenv("GITHUB_ACTOR") ?: localProperties.getProperty("github.username")
                    val githubToken = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("github.personalAccessToken")

                    if (githubUsername.isNullOrBlank() || githubToken.isNullOrBlank()) {
                        error(
                            """
                                GitHub credentials not found in environment variables or local.properties
                                Please set the following environment variables or add them to your local.properties file:
                                
                                Environment variables:
                                GITHUB_ACTOR=YOUR_GITHUB_USERNAME (it defined by default on Github Actions)
                                GITHUB_TOKEN=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN or secrets.GITHUB_TOKEN on GitHub Actions
                                
                                Or in local.properties:
                                github.username=YOUR_GITHUB_USERNAME
                                github.personalAccessToken=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
                                
                                Note: The Personal Access Token must have the 'write:packages' scope.
                                See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens
                            """.trimIndent()
                        )
                    }

                    val mavenArtifactRepository = repositories.getByName("GitHubPackages") as MavenArtifactRepository
                    mavenArtifactRepository.credentials.username = githubUsername
                    mavenArtifactRepository.credentials.password = githubToken
                }
            }
        }
    }

    private fun loadLocalProperties(rootProject: Project): Properties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }
}

private fun Project.publishing(action: PublishingExtension.() -> Unit) = extensions.configure(action)
