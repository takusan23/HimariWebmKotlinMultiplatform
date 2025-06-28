package io.github.takusan23.himariwebmkotlinmultiplatform.extension

/**
 * リスト in リスト
 * true が来たら in リストの中を空にする
 */
internal fun <T> Iterable<T>.listInListIfTrue(predicate: (T) -> Boolean): List<List<T>> {
    val listInList = arrayListOf<List<T>>()
    val inList = arrayListOf<T>()
    this.forEach { item ->
        if (predicate(item)) {
            listInList.add(inList.toList())
            inList.clear()
        }
        inList += item
    }

    // 最後
    listInList.add(inList.toList())
    return listInList
}