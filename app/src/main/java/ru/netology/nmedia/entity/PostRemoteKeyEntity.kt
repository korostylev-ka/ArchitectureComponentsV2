package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

//класс для хранения ключей
@Entity
data class PostRemoteKeyEntity(
    @PrimaryKey
    val type: KeyType,
    val id: Long,
) {
    enum class KeyType {
        //пост, который находится в самоме вверху и в самом низу
        AFTER, BEFORE
    }
}

