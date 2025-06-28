package io.github.takusan23.himariwebmkotlinmultiplatform.tool

/**
 * EBMLの要素を表すデータクラス
 *
 * @param matroskaId [MatroskaId]。未知の ID の場合は null
 * @param data データ
 */
data class MatroskaElement(
    val matroskaId: MatroskaId,
    val data: ByteArray
) {
    // 以下、自動生成
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MatroskaElement

        if (matroskaId != other.matroskaId) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matroskaId.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}