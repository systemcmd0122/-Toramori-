package com.example.e_zuka.data.base

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

abstract class BaseRepository(val context: Context) {
    private val cache = ConcurrentHashMap<String, CachedData<Any>>()

    protected suspend fun <T> safeApiCall(
        cacheKey: String? = null,
        cacheDuration: Long = 5, // デフォルト5分
        forceRefresh: Boolean = false,
        apiCall: suspend () -> T
    ): Flow<NetworkResult<T>> = flow {
        emit(NetworkResult.Loading)

        try {
            // キャッシュチェック
            if (!forceRefresh && cacheKey != null) {
                getCachedData<T>(cacheKey)?.let { cachedData ->
                    emit(NetworkResult.Cache(cachedData))
                    if (!isNetworkAvailable()) return@flow
                }
            }

            // ネットワーク接続チェック
            if (!isNetworkAvailable()) {
                throw NoNetworkException()
            }

            // APIコール
            val response = apiCall()

            // キャッシュ保存
            if (cacheKey != null) {
                saveToCache(cacheKey, response, cacheDuration)
            }

            emit(NetworkResult.Success(response))

        } catch (e: Exception) {
            emit(NetworkResult.Error(e, getErrorMessage(e)))
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is NoNetworkException -> "ネットワーク接続がありません"
            is FirebaseNetworkException -> "ネットワークエラーが発生しました"
            is FirebaseException -> "サーバーエラーが発生しました"
            is UnknownHostException -> "サーバーに接続できません"
            else -> "予期せぬエラーが発生しました"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCachedData(key: String): T? {
        val cachedData = cache[key] ?: return null
        if (cachedData.isExpired()) {
            cache.remove(key)
            return null
        }
        return cachedData.data as? T
    }

    private fun <T> saveToCache(key: String, data: T, durationMinutes: Long) {
        cache[key] = CachedData(data, System.currentTimeMillis() + durationMinutes.minutes.inWholeMilliseconds) as CachedData<Any>
    }

    private data class CachedData<T>(
        val data: T,
        val expirationTime: Long
    ) {
        fun isExpired() = System.currentTimeMillis() > expirationTime
    }
}

class NoNetworkException : Exception()
