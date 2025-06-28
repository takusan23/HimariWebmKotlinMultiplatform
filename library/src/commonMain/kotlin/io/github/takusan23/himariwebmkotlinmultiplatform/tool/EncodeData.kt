package io.github.takusan23.himariwebmkotlinmultiplatform.tool

/**
 * エンコードしたデータ
 *
 * @param trackIndex [extension.VIDEO_TRACK_TYPE] [extension.AUDIO_TRACK_TYPE]
 * @param time 時間
 * @param isKeyFrame キーフレームかどうか
 * @param encodeDataSize エンコードしたデータのサイズ。注意：SimpleBlock として使うためには先頭 4 バイトを追加する必要があります。
 * @param encodeData エンコードしたデータ
 */
data class EncodeData(
    val trackIndex: Int,
    val time: Long,
    val isKeyFrame: Boolean,
    val encodeDataSize: Int,
    val encodeData: ByteArray
)