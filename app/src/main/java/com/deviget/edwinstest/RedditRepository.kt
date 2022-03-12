package com.deviget.edwinstest

import android.util.Log
import com.deviget.edwinstest.api.PostsPageData
import com.deviget.edwinstest.api.RedditApi
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

class RedditRepository {
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("Ktor Logger ->", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    private val redditApi = RedditApi(httpClient)

    suspend fun fetchPostsPage(after: String): PostsPageData {
        val accessToken = redditApi.requestAccessToken().accessToken
        return redditApi.getTopPostsPage(accessToken, after).data
    }

    suspend fun downloadFile(
        url: String,
        file: File,
        callback: suspend (boolean: Boolean, file: File) -> Unit
    ) {
        httpClient.downloadFile(url, file, callback)
    }

    private suspend fun HttpClient.downloadFile(
        url: String,
        file: File,
        callback: suspend (boolean: Boolean, file: File) -> Unit
    ) {
        val call = this.request<HttpResponse> {
            url(url)
            method = HttpMethod.Get
        }
        val downloadSuccessful = call.status.isSuccess()
        if (downloadSuccessful) {
            call.content.copyAndClose(file.writeChannel())
        }
        return callback(downloadSuccessful, file)
    }
}
