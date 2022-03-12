package com.deviget.edwinstest

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deviget.edwinstest.api.PostData
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
import kotlinx.serialization.ExperimentalSerializationApi
import java.lang.ref.WeakReference

class RedditPostsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RedditPostsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RedditPostsViewModel(WeakReference(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RedditPostsViewModel(private val contextRef: WeakReference<Context>) : ViewModel() {
    private val _uiState = MutableStateFlow<Result<List<PostWrapper>>>(Result.Loading(false))
    val uiState: StateFlow<Result<List<PostWrapper>>> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    private var after: String = ""

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
        fetchPostsPage()
    }

    fun fetchPostsPage() {
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            try {
                _uiState.update {
                    Result.Loading(true)
                }

                val accessToken = redditApi.requestAccessToken().accessToken

                val pageData = redditApi.getTopPostsPage(accessToken, after).data

                val newPostWrapperItems = pageData.children

                contextRef.get()?.let { context ->
                    val sharedPref =
                        context.getSharedPreferences("reddit-client", Context.MODE_PRIVATE)
                    val storedReadPosts: MutableSet<String> =
                        sharedPref?.getStringSet("read-posts", mutableSetOf()) ?: mutableSetOf()

                    for (currentPost in newPostWrapperItems) {
                        val postWasRead = storedReadPosts.contains(currentPost.data.id)

                        currentPost.data.isRead = postWasRead
                    }
                }

                after = pageData.after

                _uiState.update {
                    Result.Success(newPostWrapperItems)
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

    @ExperimentalSerializationApi
    fun savePostAsRead(postData: PostData) {
        postData.isRead = true
        _uiState.update {
            _uiState.value
        }

        contextRef.get()?.let { context ->
            val sharedPref = context.getSharedPreferences("reddit-client", Context.MODE_PRIVATE)

            val storedReadPosts: MutableSet<String> =
                sharedPref.getStringSet("read-posts", mutableSetOf()) ?: mutableSetOf()

            // We need a copy of the stored set in order to safely add more items without referencing
            // the original. More info: https://stackoverflow.com/a/14034804
            val newReadPosts = mutableSetOf<String>()
            newReadPosts.addAll(storedReadPosts)
            newReadPosts.add(postData.id)

            sharedPref.edit().putStringSet("read-posts", newReadPosts).apply()
        }
    }

    fun removePostIdFromDataSet(postIdToDelete: String) {
        (_uiState.value as? Result.Success)?.let { value ->
            val reducedList = value.data.toMutableList()
            // Delete using post id and not recycler view position, to avoid any edge case of
            // queued animation delays making us target the wrong post for deletion here:
            reducedList.removeIf { item -> item.data.id == postIdToDelete }
            _uiState.update {
                Result.Success(reducedList)
            }
        }
    }

    fun clearDataSet() {
        _uiState.update {
            Result.Success(emptyList())
        }
    }

    fun hasData(): Boolean {
        (_uiState.value as? Result.Success)?.let { value ->
            return value.data.isNotEmpty()
        }

        return false
    }
}
