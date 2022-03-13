package com.deviget.edwinstest.common

/**
 * A generic class that holds a value with its loading status.
 * "MyResult" name is just to avoid any confusion with "Result" class from the Kotlin Standard
 * Library, or any other.
 */
sealed class MyResult<out T> {
    data class Success<out T>(val data: T) : MyResult<T>()
    data class Loading(val loading: Boolean) : MyResult<Nothing>()
    data class Error(val errorMessage: String) : MyResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<T> -> "Success[data=$data]"
            is Loading -> "Loading[loading=$loading]"
            is Error -> "Error[errorMessage=$errorMessage]"
        }
    }
}