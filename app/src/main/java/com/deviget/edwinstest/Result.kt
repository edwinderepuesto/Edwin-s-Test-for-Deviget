package com.deviget.edwinstest

/**
 * A generic class that holds a value with its loading status.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Loading(val loading: Boolean) : Result<Nothing>()
    data class Error(val errorMessage: String) : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<T> -> "Success[data=$data]"
            is Loading -> "Loading[loading=$loading]"
            is Error -> "Error[errorMessage=$errorMessage]"
        }
    }
}