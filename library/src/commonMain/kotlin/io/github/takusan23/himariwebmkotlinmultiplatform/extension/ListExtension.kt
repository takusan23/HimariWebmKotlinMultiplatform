package io.github.takusan23.himariwebmkotlinmultiplatform.extension

/**
 * リスト in リスト
 * true が来たら in リストの中を空にする
 */
internal fun <T> Iterable<T>.listInListIfTrue(predicate: (T) -> Boolean): List<List<T>> {
    val listInList = arrayListOf<List<T>>()
    val inList = arrayListOf<T>()

    // 最初の true を無視する
    var isFirstTrue = true

    this.forEach { item ->
        if (isFirstTrue) {
            if (predicate(item)) {
                isFirstTrue = false
            }
        } else {
            if (predicate(item)) {
                listInList.add(inList.toList())
                inList.clear()
            }
        }
        inList += item
    }

    // 最後
    // ない場合は追加しない
    if (inList.isNotEmpty()) {
        listInList.add(inList.toList())
    }
    return listInList
}
