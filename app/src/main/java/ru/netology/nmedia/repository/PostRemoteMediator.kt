package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.Response
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

//экспериментальная аннотация, чтобы код скомплеировался
@OptIn(ExperimentalPagingApi::class)
//переименовываем PostPagingSource в
class PostRemoteMediator(
    private val service: ApiService,
    private val db: AppDb,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
) : RemoteMediator<Int, PostEntity>() {
    override suspend fun load(
        //действие, которое хочет совершить пользователь(обновить, скроллить вверх или вниз)
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                //достаем размер страниц из экземпляра PagingState, config.pagesize
                LoadType.REFRESH -> {
                    service.getLatest(state.config.pageSize)
                    /*val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                        //конец страницы не достигнут
                        endOfPaginationReached = false
                    )
                    println("ID = $id")
                    service.getAfter(id, state.config.pageSize)*/

                }
                //скроллинг вверх(запрос на получение верхней страницы). Ключ достаем из аргумента state(достаем последний элемент)
                LoadType.PREPEND -> {
                    //val id = state.firstItemOrNull()?.id ?: return MediatorResult.Success(false)

                    /*меняем на чтение из базы данных
                    val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                        //конец страницы не достигнут
                        endOfPaginationReached = false
                    )
                    service.getAfter(id, state.config.pageSize)*/

                    //убираем автоматический запрос при скролле вверх
                    return MediatorResult.Success(true)

                }
                //скроллинг вниз. Запрос на получение нижней страницы
                LoadType.APPEND -> {
                    /* Чтение первого поста из оперативной памяти
                    val id = state.lastItemOrNull()?.id ?: return MediatorResult.Success(false) //конец страницы не достигнут */
                    //меняем на чтение из базы данных
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    service.getBefore(id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )

            //заполняем таблицу ключей данными которые приходят про сети
            db.withTransaction {
                //тип входных данных
                when (loadType) {
                    LoadType.REFRESH -> {
                        //получаем id последнего поста в базе из базы данных кючей
                        val idLast = postRemoteKeyDao.max()
                        //очищаем базу данных с ключами
                        postRemoteKeyDao.removeAll()
                        //записываем список ключей, первого и последнего поста
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id,
                                ),
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    /* Меняем ключ самого старого поста в базе ключей с самого старого
                                     во всем списке, пришедшего с сервера, на ключ самого свежего поста,
                                     кторый есть в базе данных кдючей
                                    id = body.last().id,*/
                                   id = idLast ?: body.last().id
                                ),
                            )
                        )
                        /*убираем очищение списка постов в базе
                        postDao.removeAll()*/
                    }
                    //при скролле наверх запишем только ключ after
                    LoadType.PREPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.AFTER,
                                id = body.first().id,
                            )
                        )
                    }
                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                id = body.last().id,
                            )
                        )
                    }
                }
                //запишем в базу список элементов, который "пришел"
                postDao.insert(body.toEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = body.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}