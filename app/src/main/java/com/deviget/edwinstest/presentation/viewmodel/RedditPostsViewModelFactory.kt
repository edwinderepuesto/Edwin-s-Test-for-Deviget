package com.deviget.edwinstest.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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