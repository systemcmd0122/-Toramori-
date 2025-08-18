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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProblemViewModel(context: Context) : ViewModel() {
    private val repository = RegionProblemRepository(context)

    // UIの状態を管理するデータクラス
    data class UiState(
        val problems: List<ProblemData> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val lastUpdateTime: Long = 0L
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 問題一覧の取得（キャッシング対応）
    fun loadProblems(regionCodeId: String) {
        viewModelScope.launch {
            repository.getProblemsFlow(regionCodeId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(
                            isLoading = !_uiState.value.problems.any(),
                            isRefreshing = _uiState.value.problems.any()
                        ) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(
                            problems = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastUpdateTime = System.currentTimeMillis()
                        ) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message ?: "データの取得に失敗しました"
                        ) }
                    }
                    is NetworkResult.Cache -> {
                        if (_uiState.value.problems.isEmpty()) {
                            _uiState.update { it.copy(
                                problems = result.data,
                                errorMessage = null
                            ) }
                        }
                    }
                }
            }
        }
    }

    // 問題に対する助けの登録
    fun helpProblem(problemId: String, helperUserId: String) {
        viewModelScope.launch {
            when (val result = repository.helpProblem(problemId, helperUserId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(
                        successMessage = "助けることを登録しました",
                        errorMessage = null
                    ) }
                    // 一覧を更新（キャッシュ更新）
                    val currentProblem = _uiState.value.problems.find { it.problemId == problemId }
                    currentProblem?.let { loadProblems(it.regionCodeId) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(
                        errorMessage = result.message ?: "助ける登録に失敗しました"
                    ) }
                }
                else -> {} // Loading, Cacheは処理不要
            }
        }
    }

    // 問題を解決済みにする
    fun solveProblem(problemId: String) {
        viewModelScope.launch {
            when (val result = repository.solveProblem(problemId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(
                        successMessage = "困りごとを解決済みにしました",
                        errorMessage = null
                    ) }
                    // 一覧を更新（キャッシュ更新）
                    val currentProblem = _uiState.value.problems.find { it.problemId == problemId }
                    currentProblem?.let { loadProblems(it.regionCodeId) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(
                        errorMessage = result.message ?: "解決処理に失敗しました"
                    ) }
                }
                else -> {} // Loading, Cacheは処理不要
            }
        }
    }

    // メッセージのクリア
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

}
