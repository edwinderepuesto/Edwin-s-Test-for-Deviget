package com.deviget.edwinstest.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostsPage(
    @SerialName("data")
    val data: PostsPageData,
)

@Serializable
data class PostsPageData(
    @SerialName("children")
    val children: List<PostWrapper>,
)

@Serializable
data class PostWrapper(
    @SerialName("data")
    val data: PostData,
)

@Serializable
data class PostData(
    @SerialName("id")
    val id: String,

    @SerialName("title")
    val title: String,

    @SerialName("selftext")
    val selfText: String,
)