package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType
//объединяем объекты Пост и разделители общим классом
sealed class FeedItem{
    abstract val id: Long
}

//data класс для разделителей
data class DateSeparator(
    override val id: Long,
    //ссылка на картинку
    //val image: String,
) : FeedItem()

data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
): FeedItem()

data class Attachment(
    val url: String,
    val type: AttachmentType,
)
