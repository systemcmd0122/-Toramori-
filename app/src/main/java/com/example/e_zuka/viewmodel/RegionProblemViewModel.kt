package com.example.e_zuka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.base.NetworkResult
import com.example.e_zuka.data.model.ProblemData
import com.example.e_zuka.data.region.RegionProblemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegionProblemViewModel(context: Context) : ViewModel() {
    private val repository = RegionProblemRepository(context)

    private val _problems = MutableStateFlow<List<ProblemData>>(emptyList())
    val problems: StateFlow<List<ProblemData>> = _problems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadProblems(regionCodeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getProblems(regionCodeId)
                _problems.value = list
            } catch (e: Exception) {
                _errorMessage.value = "困りごとの取得に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun postProblem(problem: ProblemData) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.postProblem(problem)) {
                    is NetworkResult.Success -> {
                        _successMessage.value = "困りごとを投稿しました"
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message ?: "投稿に失敗しました"
                    }
                    else -> {
                        _errorMessage.value = "投稿に失敗しました"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "投稿に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun helpProblem(problemId: String, helperUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.helpProblem(problemId, helperUserId)) {
                    is NetworkResult.Success -> {
                        _successMessage.value = "助けることに登録しました"
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message ?: "助ける登録に失敗しました"
                    }
                    else -> {
                        _errorMessage.value = "助ける登録に失敗しました"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "助ける登録に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun solveProblem(problemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.solveProblem(problemId)) {
                    is NetworkResult.Success -> {
                        _successMessage.value = "困りごとを解決済みにしました"
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message ?: "解決処理に失敗しました"
                    }
                    else -> {
                        _errorMessage.value = "解決処理に失敗しました"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "解決処理に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
}
