package com.example.e_zuka.data.base

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Exception, val message: String? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
    data class Cache<T>(val data: T) : NetworkResult<T>()

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(exception: Exception, message: String? = null) = Error(exception, message)
        fun <T> cache(data: T) = Cache(data)
        fun loading() = Loading
    }
}
