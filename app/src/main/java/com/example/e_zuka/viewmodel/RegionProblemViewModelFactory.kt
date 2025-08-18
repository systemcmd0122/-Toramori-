package com.example.e_zuka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RegionProblemViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RegionProblemViewModel::class.java) -> {
                RegionProblemViewModel(context) as T
            }
            modelClass.isAssignableFrom(ProblemViewModel::class.java) -> {
                ProblemViewModel(context) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
