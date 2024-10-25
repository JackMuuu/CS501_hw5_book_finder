package com.example.booksearchapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BookViewModel(private val repository: BookRepository) : ViewModel() {

    // State for the list of books
    private val _books = MutableStateFlow<List<Volume>>(emptyList())
    val books: StateFlow<List<Volume>> = _books

    // State for loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // State for handling potential errors
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // State for the search query
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Function to update the query
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    // Function to search for books based on the query
    fun searchBooks() {
        if (_query.value.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Collect the flow from the repository
                repository.searchBooks(_query.value).collect { response ->
                    _books.value = response.items ?: emptyList()
                    _error.value = null // Clear any previous error
                }
            } catch (e: Exception) {
                _books.value = emptyList()
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}