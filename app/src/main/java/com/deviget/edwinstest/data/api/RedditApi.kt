package com.deviget.edwinstest.data.api

import com.deviget.edwinstest.data.dto.AuthorizationResponse
import com.deviget.edwinstest.data.dto.PostsPage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class RedditApi(private val client: HttpClient) {
    suspend fun requestAccessToken(): AuthorizationResponse =
        client.post("https://www.reddit.com/api/v1/access_token") {
            header(HttpHeaders.Authorization, "Basic LXFNQ1dEc3gtVFZzRGlobUFkRUxKUTo=")
            body = FormDataContent(Parameters.build {
                append("grant_type", "https://oauth.reddit.com/grants/installed_client")
                append("device_id", "NON_TRACKED_DEVICE_ID")
            })
        }

    suspend fun getTopPostsPage(accessToken: String, after: String): PostsPage =
        client.get("https://oauth.reddit.com/top?after=$after") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
}