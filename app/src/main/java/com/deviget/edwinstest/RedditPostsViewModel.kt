package com.deviget.edwinstest

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deviget.edwinstest.api.PostData
import com.deviget.edwinstest.api.PostWrapper
import com.deviget.edwinstest.common.MyResult
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
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
    private val _uiState = MutableStateFlow<MyResult<List<PostWrapper>>>(MyResult.Loading(false))
    val uiState: StateFlow<MyResult<List<PostWrapper>>> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    private var after: String = ""

    private val repository = RedditRepository()

    init {
        fetchPostsPage(resetData = true)
    }

    fun fetchPostsPage(resetData: Boolean) {
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            try {
                // Only populated when needed:
                var onlyPreviousPosts = emptyList<PostWrapper>()

                if (resetData) {
                    // Move to first page if resetting data:
                    after = ""
                } else {
                    // Otherwise, keep the previous posts so we can append the fresh ones to them:
                    (_uiState.value as? MyResult.Success)?.let { value ->
                        onlyPreviousPosts = value.data
                    }
                }

                _uiState.update {
                    MyResult.Loading(true)
                }

                val newPageData = repository.fetchPostsPage(after)

                val onlyNewPosts = newPageData.children

                // Mark fetched posts as read if previously cached as such:
                contextRef.get()?.let { context ->
                    val sharedPref =
                        context.getSharedPreferences("reddit-client", Context.MODE_PRIVATE)
                    val storedReadPosts: MutableSet<String> =
                        sharedPref?.getStringSet("read-posts", mutableSetOf()) ?: mutableSetOf()

                    for (currentPost in onlyNewPosts) {
                        val postWasRead = storedReadPosts.contains(currentPost.data.id)

                        currentPost.data.isRead = postWasRead
                    }
                }

                // Mark next page for later:
                after = newPageData.after

                // Unify all required posts and update UI:
                val previousAndNewPosts = mutableListOf<PostWrapper>()
                previousAndNewPosts.addAll(onlyPreviousPosts)
                previousAndNewPosts.addAll(onlyNewPosts)
                _uiState.update {
                    MyResult.Success(previousAndNewPosts)
                }
            } catch (ioException: IOException) {
                Log.d("ktor", "Error fetching posts:")
                ioException.printStackTrace()
                _uiState.update {
                    MyResult.Error(ioException.message ?: "Unknown error")
                }
            }
        }
    }

    @ExperimentalSerializationApi
    fun savePostAsRead(postData: PostData) {
        postData.isRead = true

        // Force UI update to reflect unread status change on tablets:
        (_uiState.value as? MyResult.Success)?.let { value ->
            val currentData = value.data
            _uiState.update { MyResult.Loading(false) }
            _uiState.update { MyResult.Success(currentData) }
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
        (_uiState.value as? MyResult.Success)?.let { value ->
            val reducedList = value.data.toMutableList()
            // Delete using post id and not recycler view position, to avoid any edge case of
            // queued animation delays making us target the wrong post for deletion here:
            reducedList.removeIf { item -> item.data.id == postIdToDelete }
            _uiState.update {
                MyResult.Success(reducedList)
            }
        }
    }

    fun clearDataSet() {
        _uiState.update {
            MyResult.Success(emptyList())
        }
    }

    suspend fun downloadFile(url: String, postId: String) {
        contextRef.get()?.let {
            val file =
                File(
                    it.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "reddit-$postId.jpg"
                )
            repository.downloadFile(url, file, ::onFileDownloadEnded)
        }
    }

    // Shows Toast with the result of the download operation and tries to open the file
    // in the default viewer app in the device:
    private fun onFileDownloadEnded(wasSuccessful: Boolean, fileToOpen: File) {
        contextRef.get()?.let { context ->
            var stringRes: Int
            if (wasSuccessful) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                            FileProvider.getUriForFile(
                                context,
                                BuildConfig.APPLICATION_ID + ".provider",
                                fileToOpen
                            ),
                            "image/*"
                        ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    context.startActivity(intent)
                    stringRes = R.string.file_downloaded
                } catch (e: Exception) {
                    stringRes = R.string.error_opening_file
                }
            } else {
                stringRes = R.string.download_failed
            }

            Toast.makeText(
                context,
                context.getString(stringRes),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
