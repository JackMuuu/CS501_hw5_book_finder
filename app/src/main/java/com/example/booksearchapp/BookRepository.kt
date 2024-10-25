package com.example.booksearchapp

import com.example.booksearchapp.VolumeResponse
import com.example.booksearchapp.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookRepository {
    fun searchBooks(query: String): Flow<VolumeResponse> = flow {
        val response = RetrofitInstance.api.searchBooks(query)
        emit(response)
    }
}