package com.example.utpstudywork.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.utpstudywork.data.di.UseCases

class NotesViewModelFactory(private val use: UseCases) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotesViewModel(use) as T
    }
}
