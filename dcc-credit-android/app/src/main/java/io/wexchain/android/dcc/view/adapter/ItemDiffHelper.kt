package io.wexchain.android.dcc.view.adapter

import android.support.annotation.NonNull
import android.support.v4.util.ObjectsCompat
import android.support.v7.util.DiffUtil

/**
 * Created by sisel on 2018/3/12.
 */
inline fun <T> itemDiffCallback(
    crossinline compareId: (T, T) -> Boolean,
    crossinline compareContent: (T, T) -> Boolean = { a, b ->
        ObjectsCompat.equals(a, b)
    }
): DiffUtil.ItemCallback<T> {
    return object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(@NonNull oldItem: T, @NonNull newItem: T): Boolean {
            return compareId(oldItem, newItem)
        }

        override fun areContentsTheSame(@NonNull oldItem: T, @NonNull newItem: T): Boolean {
            return compareContent(oldItem, newItem)
        }

    }
}

fun <T> defaultItemDiffCallback(): DiffUtil.ItemCallback<T> {
    return object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(@NonNull oldItem: T, @NonNull newItem: T): Boolean {
            return areContentsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(@NonNull oldItem: T, @NonNull newItem: T): Boolean {
            return ObjectsCompat.equals(oldItem, newItem)
        }

    }
}

inline fun <T> diffCallbackById(crossinline getId: (T) -> Long): DiffUtil.ItemCallback<T> {
    return itemDiffCallback({ a, b ->
        getId(a) == getId(b)
    })
}