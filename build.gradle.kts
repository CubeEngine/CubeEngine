import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpResponse.BodySubscribers
import java.util.concurrent.ForkJoinPool

group = "org.cubeengine"
version = "1.0.0-SNAPSHOT"

description = "CubeEngine Project"
data class IdResponse(val id: Int?)
data class AssetResponse(val id: Int?, val name: String)

data class CreateReleaseBody(
    val tag_name: String,
    val commitish: String,
    val name: String,
    val body: String,
    val draft: Boolean = false,
    val generate_release_notes: Boolean = false,
)

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        val jacksonVersion = "2.16.0"
        classpath("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    }
}

inline fun <reified T> ObjectMapper.decoding(): BodyHandler<T> {
    val ref = object : TypeReference<T>() {}
    return BodyHandler<T> { info ->
        BodySubscribers.mapping(BodySubscribers.ofByteArray()) {
            // println("Request output: ${info.statusCode()} -> ${String(it)}")
            this@decoding.readValue(it, ref)
        }
    }
}

fun authenticatedRequest(to: String): HttpRequest.Builder {
    val builder = HttpRequest.newBuilder()
    builder.uri(URI(to))
    val token = project.findProperty("githubApiToken") ?: System.getenv("GITHUB_API_TOKEN")?.ifBlank { null }
    if (token != null) {
        builder.setHeader("Authorization", "Bearer $token")
    }
    return builder
}

inline fun <reified T : Any> HttpClient.getFrom(url: String, objectMapper: ObjectMapper): T {
    val req = authenticatedRequest(url).GET().build()

    return send(req, objectMapper.decoding<T>()).body()
}

fun <T> ObjectMapper.encoding(value: T): BodyPublisher {
    return BodyPublishers.ofByteArray(writeValueAsBytes(value))
}

inline fun withLimitedConcurrency(n: Int, crossinline block: () -> Unit) {
    ForkJoinPool(
        n,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        true,
        n,
        n,
        0,
        { true },
        30,
        TimeUnit.SECONDS,
    ).submit {
        block()
    }.join()
}

val updateGithubRelease by tasks.registering {
    group = "publishing"

    val spongeVersion: String by project
    val spongeApiVersion = spongeVersion.split('.').first()

    for (subproject in project.subprojects) {
        mustRunAfter(subproject.tasks.withType<Jar>())
    }

    doLast {
        val files = project.subprojects
            .asSequence()
            .filterNot { it.findProperty("pluginPublished") == "false" }
            .flatMap { it.tasks.withType<Jar>() }
            .filter { it.didWork }
            .filter {
                // artifacts of interest are always those without a classifier
                it.archiveClassifier.get().isEmpty()
            }
            .mapNotNull { it.archiveFile.orNull }
            .map { it.asFile }
            .associate { it.name to it.toPath() }

        if (project.findProperty("githubPublishDryRun") == "true") {
            for ((name, file) in files) {
                println("File for $name: $file")
            }
            return@doLast
        }

        val basePath = "/repos/CubeEngine/CubeEngine/releases"
        val apiBaseUrl = "https://api.github.com$basePath"
        val uploadsBaseUrl = "https://uploads.github.com$basePath"

        val httpClient: HttpClient = HttpClient.newBuilder()
            .build()

        val tagName = "snapshots-$spongeApiVersion"
        val objectMapper = ObjectMapper().also {
            it.registerKotlinModule()
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        val releaseId = httpClient.getFrom<IdResponse>("$apiBaseUrl/tags/$tagName", objectMapper).id
            ?: run {
                val releaseCommitish = "tag"
                val releaseName = "Snapshots for Sponge $spongeApiVersion"
                val releaseDescription = "..."
                val releaseData = CreateReleaseBody(tagName, releaseCommitish, releaseName, releaseDescription)
                val createRelease = authenticatedRequest(apiBaseUrl).POST(objectMapper.encoding(releaseData)).build()
                httpClient.send(createRelease, objectMapper.decoding<IdResponse>()).body().id
            }
        if (releaseId == null) {
            error("Could not Create Release! :(")
        }

        withLimitedConcurrency(2) {
            httpClient.getFrom<List<AssetResponse>>("$apiBaseUrl/${releaseId}/assets?per_page=100", objectMapper)
                .parallelStream()
                .forEach { asset ->
                    val deleteAsset = authenticatedRequest("$apiBaseUrl/assets/${asset.id}").DELETE().build()
                    val deleteResponse = httpClient.send(deleteAsset, BodyHandlers.discarding())
                    if (deleteResponse.statusCode() != 204) {
                        error("Could not delete Asset for $name")
                    }
                }


            files.toList().parallelStream().forEach { (name, path) ->
                val uploadAsset = authenticatedRequest("$uploadsBaseUrl/${releaseId}/assets?name=${name}")
                    .setHeader("Content-Type", "application/octet-stream")
                    .POST(BodyPublishers.ofFile(path)).build()
                val assetResponse = httpClient.send(uploadAsset, objectMapper.decoding<IdResponse>()).body()
                if (assetResponse.id == null) {
                    error("Could not Upload File! :(")
                }
            }
        }
    }
}
