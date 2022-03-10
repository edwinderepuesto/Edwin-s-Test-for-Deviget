package com.deviget.edwinstest

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deviget.edwinstest.api.PostWrapper
import com.deviget.edwinstest.api.RedditApi
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RedditPostsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<Result<List<PostWrapper>>>(Result.Loading(false))
    val uiState: StateFlow<Result<List<PostWrapper>>> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    private val redditApi = RedditApi(HttpClient {
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
    })

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            try {
                _uiState.update {
                    Result.Loading(true)
                }

                val accessToken = redditApi.requestAccessToken().accessToken

                val postWrapperItems = redditApi.getOverallTopPosts(accessToken).data.children

                _uiState.update {
                    Result.Success(postWrapperItems)
                }
            } catch (ioException: IOException) {
                Log.d("ktor", "Error fetching posts:")
                ioException.printStackTrace()
                _uiState.update {
                    Result.Error(ioException.message ?: "Unknown error")
                }
            }
        }
    }
}
