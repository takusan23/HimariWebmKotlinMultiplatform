package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.extension.AUDIO_TRACK_TYPE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.SIMPLE_BLOCK_FLAGS_KEYFRAME
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.UNKNOWN_DATA_SIZE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.VIDEO_TRACK_TYPE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.analyzeUnknownDataSizeForClusterData
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.analyzeUnknownDataSizeForSegmentData
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.andFF
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.filterId
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.getDataSize
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.getElementLength
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toInt
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toMatroskaId
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.EncodeData
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.MatroskaElement
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.SpecMatroskaId
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.WebmParseResult

object HimariWebmParser {

    /**
     * WebM をパースする。データクラス版
     *
     * @param webmFileByteArray WebM ファイル
     * @return [WebmParseResult]
     */
    fun parseWebm(webmFileByteArray: ByteArray): WebmParseResult {
        // 配列版を取得
        val parse = parseWebmLowLevel(webmFileByteArray)

        // トラックが存在するか
        val videoTrack = parse.filterId(SpecMatroskaId.TrackType).indexOfFirst { it.data.contentEquals(VIDEO_TRACK_TYPE.toByteArray()) }.takeIf { it != -1 }
        val audioTrack = parse.filterId(SpecMatroskaId.TrackType).indexOfFirst { it.data.contentEquals(AUDIO_TRACK_TYPE.toByteArray()) }.takeIf { it != -1 }

        // あれば取得
        val audioOrNull = if (audioTrack != null) {
            WebmParseResult.AudioTrack(
                audioCodec = parse.filterId(SpecMatroskaId.CodecID)[audioTrack].data,
                audioSamplingRate = Float.fromBits(parse.filterId(SpecMatroskaId.SamplingFrequency).first().data.toInt()),
                audioChannelCount = parse.filterId(SpecMatroskaId.Channels).first().data.toInt()
            )
        } else null
        val videoOrNull = if (videoTrack != null) {
            WebmParseResult.VideoTrack(
                videoCodec = parse.filterId(SpecMatroskaId.CodecID)[videoTrack].data,
                videoWidth = parse.filterId(SpecMatroskaId.PixelWidth).first().data.toInt(),
                videoHeight = parse.filterId(SpecMatroskaId.PixelHeight).first().data.toInt()
            )
        } else null

        // エンコードしたデータ
        val encodeDataList = arrayListOf<EncodeData>()
        var latestClusterTimestamp = 0L
        parse.forEach { (matroskaId, data) ->

            // ClusterTimestamp が来たら時間を控える。SimpleBlock の時間が相対時間のため、
            if (matroskaId == SpecMatroskaId.Timestamp) {
                latestClusterTimestamp = data.toInt().toLong()
            }

            // エンコードされたデータは SimpleBlock
            if (matroskaId == SpecMatroskaId.SimpleBlock) {
                // 先頭にトラック番号、Timestamp からの相対時間（2バイト）、キーフレームかどうか
                // トラック番号は VINT 方式
                // キーフレームかは Byte->Int で符号付きになるので
                val trackIndex = byteArrayOf(data[0]).getDataSize()
                val offsetTime = byteArrayOf(data[1], data[2]).toInt().toLong()
                val isKeyFrame = data[3].toInt().andFF() == SIMPLE_BLOCK_FLAGS_KEYFRAME

                encodeDataList += EncodeData(
                    trackIndex = trackIndex,
                    time = latestClusterTimestamp + offsetTime,
                    isKeyFrame = isKeyFrame,
                    encodeDataSize = data.size - 4, // 実際のデータは 4 バイトスキップした値
                    encodeData = data.drop(4).toByteArray()
                )
            }
        }

        // トラック番号を取る
        // 多分 videoTrack + 1 したのがそうだと思うけど、書き込みアプリが何してるか分からないので
        val videoTrackIndex = videoTrack?.let { parse.filterId(SpecMatroskaId.TrackNumber)[it].data.toInt() }
        val audioTrackIndex = audioTrack?.let { parse.filterId(SpecMatroskaId.TrackNumber)[it].data.toInt() }

        return WebmParseResult(
            audioOrNull,
            videoOrNull,
            audioEncodeData = encodeDataList.filter { it.trackIndex == audioTrackIndex }.ifEmpty { null },
            videoEncodeData = encodeDataList.filter { it.trackIndex == videoTrackIndex }.ifEmpty { null }
        )
    }

    /**
     * WebM をパースする。[MatroskaElement]の配列版
     *
     * @param webmFileByteArray WebM ファイル
     * @return [MatroskaElement]の配列
     */
    fun parseWebmLowLevel(webmFileByteArray: ByteArray): List<MatroskaElement> {

        /**
         * [byteArray] から次の EBML のパースを行う
         *
         * @param byteArray EBML の[ByteArray]
         * @param startPosition EBML 要素の開始位置
         * @return パース結果[MatroskaElement]と、パースしたデータの位置
         */
        fun parseEbmlElement(byteArray: ByteArray, startPosition: Int): Pair<MatroskaElement, Int> {
            var currentPosition = startPosition

            // ID をパース
            val idLength = byteArray[currentPosition].getElementLength()
            val idByteArray = byteArray.copyOfRange(currentPosition, currentPosition + idLength)
            val matroskaId = idByteArray.toMatroskaId()
            currentPosition += idLength

            // DataSize をパース
            val dataSizeLength = byteArray[currentPosition].getElementLength()
            // JavaScript の MediaRecorder は Segment / Cluster が DataSize が長さ不明になる
            val dataSize = byteArray.copyOfRange(currentPosition, currentPosition + dataSizeLength).getDataSize().let { dataSizeOrUnknown ->
                if (dataSizeOrUnknown == -1) {
                    // 長さ不明の場合は上から舐めて出す
                    val dataByteArray = byteArray.copyOfRange(currentPosition + UNKNOWN_DATA_SIZE.size, byteArray.size)
                    when (matroskaId) {
                        SpecMatroskaId.Segment -> dataByteArray.analyzeUnknownDataSizeForSegmentData()
                        SpecMatroskaId.Cluster -> dataByteArray.analyzeUnknownDataSizeForClusterData()
                        else -> throw RuntimeException("Cluster Segment 以外は長さ不明に対応していません；；")
                    }
                } else {
                    // 長さが求まっていればそれを使う
                    dataSizeOrUnknown
                }
            }
            currentPosition += dataSizeLength

            // Data を取り出す
            val dataByteArray = byteArray.copyOfRange(currentPosition, currentPosition + dataSize)
            currentPosition += dataSize

            // 返す
            val matroskaElement = MatroskaElement(matroskaId, dataByteArray)
            return matroskaElement to currentPosition
        }

        /**
         * [byteArray] から EBML 要素をパースする。
         * 親要素の場合は子要素まで再帰的に見つける。
         *
         * 一つだけ取りたい場合は [parseEbmlElement]
         * @param byteArray WebM のバイト配列
         * @return 要素一覧
         */
        fun parseAllEbmlElement(byteArray: ByteArray): List<MatroskaElement> {
            var readPosition = 0
            val elementList = arrayListOf<MatroskaElement>()

            while (true) {
                // 要素を取得し位置を更新
                val (element, currentPosition) = parseEbmlElement(byteArray, readPosition)
                elementList += element
                readPosition = currentPosition

                // 親要素の場合は子要素の解析をする
                if ((element.matroskaId as? SpecMatroskaId)?.isParent == true) {
                    val children = parseAllEbmlElement(element.data)
                    elementList += children
                }

                // 次のデータが 3 バイト以上ない場合は break（解析できない）
                // 3 バイトの理由ですが ID+DataSize+Data それぞれ1バイト以上必要なので
                if (byteArray.size < readPosition + 3) {
                    break
                }
            }

            return elementList
        }

        // WebM の要素を全部パースする
        return parseAllEbmlElement(webmFileByteArray)
    }

}