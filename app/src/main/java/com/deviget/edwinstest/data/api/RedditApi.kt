package com.deviget.edwinstest.data.api

import com.deviget.edwinstest.common.Constants
import com.deviget.edwinstest.data.dto.AuthorizationResponse
import com.deviget.edwinstest.data.dto.PostsPage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class RedditApi(private val client: HttpClient) {
    suspend fun requestAccessToken(): AuthorizationResponse =
        client.post(Constants.REDDIT_ACCESS_TOKEN_URL) {
            header(
                HttpHeaders.Authorization,
                "Basic " + Constants.REDDIT_ENCODED_BASIC_AUTHENTICATION
            )
            body = FormDataContent(Parameters.build {
                append("grant_type", Constants.REDDIT_OAUTH_URL + "/grants/installed_client")
                append("device_id", Constants.NON_TRACKED_DEVICE_ID)
            })
        }

    suspend fun getTopPostsPage(accessToken: String, after: String): PostsPage =
        client.get(Constants.REDDIT_OAUTH_URL + "/top?after=$after") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
}