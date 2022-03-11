package com.deviget.edwinstest.api

import android.text.format.DateUtils
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

    @SerialName("created")
    val createdAt: Double,

    @SerialName("author")
    val authorName: String,

    @SerialName("title")
    val title: String,

    @SerialName("selftext")
    val selfText: String,

    @SerialName("url")
    val url: String,

    @SerialName("thumbnail")
    val thumbnailUrl: String,

    @SerialName("num_comments")
    val numberOfComments: Int,
) {
    var isRead: Boolean = false

    fun getDisplayRelativeCreationTime(): CharSequence {
        return DateUtils.getRelativeTimeSpanString(
            (createdAt * 1000).toLong(),
            System.currentTimeMillis(),
            1,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )

    }

    fun getDisplayCommentCount(): String {
        return if (numberOfComments == 1)
            "1 comment"
        else
            "$numberOfComments comments"
    }
}