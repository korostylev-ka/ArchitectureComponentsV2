package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.SeparatorDateItemBinding
import ru.netology.nmedia.dto.DateSeparator
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.view.loadCircleCrop

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
}
private val typeSepararor = 0
private val typePost = 1

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
    //меняем PostViewHolder на базовый RecyclerView.ViewHolder и Post на FeedItem
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback()) {
    //получаем тип элемента
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            //если тип рекламы, то ссылка на макет с рекламой
            is DateSeparator -> typeSepararor
            is Post -> typePost
            null -> throw IllegalArgumentException("unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            //если разделитель по дате поста, создадим его viewHolder
            typeSepararor -> DateSeparatorViewHolder(
                SeparatorDateItemBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            typePost -> PostViewHolder(
                CardPostBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            else -> throw IllegalArgumentException("unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // FIXME: students will do in HW
        getItem(position)?.let {
            when (it) {
                //если пост, приводим к view
                // Holder'a поста
                is Post -> (holder as? PostViewHolder)?.bind(it)
                is DateSeparator -> (holder as? DateSeparatorViewHolder)?.bind(it)
                null -> throw IllegalArgumentException("unknown item type")
            }
        }
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published.toString()
            content.text = post.content
            avatar.loadCircleCrop("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}")
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    // TODO: if we don't have other options, just remove dots
                    menu.setGroupVisible(R.id.owned, post.ownedByMe)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

class DateSeparatorViewHolder(
    private val binding: SeparatorDateItemBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {
    //заполняем разделитель
    fun bind(dateSeparator: DateSeparator) {
        binding.apply {
                //в зависимости от ID будем присваивать значение текста
                separatorDescription.text = when (dateSeparator.id.toInt()) {
                    0 -> "Сегодня"
                    1 -> "Вчера"
                    2 -> "На прошлой неделе"
                    else -> "Давно"
                }
            }
        }
    }


class FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        //проверяем ситуацию, когда у поста и разделителя может быть одинаковый id
        if (oldItem::class != newItem::class) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem is DateSeparator && newItem is DateSeparator) {
            return oldItem.id == newItem.id
        }
        return oldItem == newItem
    }
}
