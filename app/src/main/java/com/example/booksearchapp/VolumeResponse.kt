package com.example.booksearchapp

import com.squareup.moshi.Json

data class VolumeResponse(
    val totalItems: Int,
    val items: List<Volume>?
)

data class Volume(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?
)

data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?,
    val large: String?,
    val small: String?,
    val medium: String?
)
