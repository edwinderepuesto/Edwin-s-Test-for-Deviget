package com.deviget.edwinstest.data.dto

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
    @SerialName("after")
    val after: String,

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

    @SerialName("url")
    val url: String,

    @SerialName("num_comments")
    val numberOfComments: Int,

    @SerialName("thumbnail")
    private val thumbnailUrl: String,
) {
    var isRead: Boolean = false

    fun getSafeThumbnailUrl(): String {
        if (thumbnailUrl.isEmpty() || thumbnailUrl == "default") {
            return "https://www.redditinc.com/assets/images/site/reddit-logo.png"
        }

        return thumbnailUrl
    }

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