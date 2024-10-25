package com.example.booksearchapp

import okhttp3.Interceptor
import okhttp3.Response
import com.example.booksearchapp.BuildConfig

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Add the API key as a query parameter
        val urlWithApiKey = originalUrl.newBuilder()
            .addQueryParameter("key", BuildConfig.GoogleBooksAPI)
            .build()

        // Build the new request with the API key
        val requestWithApiKey = originalRequest.newBuilder()
            .url(urlWithApiKey)
            .build()

        return chain.proceed(requestWithApiKey)
    }
}