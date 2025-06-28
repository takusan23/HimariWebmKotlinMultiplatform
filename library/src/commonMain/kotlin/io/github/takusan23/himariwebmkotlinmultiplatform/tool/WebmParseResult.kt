package io.github.takusan23.himariwebmkotlinmultiplatform.tool

// TODO doc 書く
data class WebmParseResult(
    val audioTrack: AudioTrack?,
    val videoTrack: VideoTrack?,
    val audioEncodeData: List<EncodeData>?,
    val videoEncodeData: List<EncodeData>?
) {

    data class AudioTrack(
        val audioCodec: ByteArray,
        val audioSamplingRate: Float,
        val audioChannelCount: Int
    ) {
        // 以下、自動生成
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as AudioTrack

            if (audioSamplingRate != other.audioSamplingRate) return false
            if (audioChannelCount != other.audioChannelCount) return false
            if (!audioCodec.contentEquals(other.audioCodec)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = audioSamplingRate.hashCode()
            result = 31 * result + audioChannelCount
            result = 31 * result + audioCodec.contentHashCode()
            return result
        }
    }

    data class VideoTrack(
        val videoCodec: ByteArray,
        val videoWidth: Int,
        val videoHeight: Int
    ) {
        // 以下、自動生成
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as VideoTrack

            if (videoWidth != other.videoWidth) return false
            if (videoHeight != other.videoHeight) return false
            if (!videoCodec.contentEquals(other.videoCodec)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = videoWidth
            result = 31 * result + videoHeight
            result = 31 * result + videoCodec.contentHashCode()
            return result
        }
    }
}