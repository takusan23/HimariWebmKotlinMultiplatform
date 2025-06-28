package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.extension.concatByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.filterId
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toEbmlByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toInt
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.MatroskaElement
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.SpecMatroskaId

object HimariFixSeekableWebm {

    /**
     * [HimariWebmParser.parseWebm]で出来た要素一覧を元に、シークできる WebM を作成する
     *
     * @param elementList [HimariWebmParser.parseWebm]
     * @return シークできる WebM のバイナリ
     */
    fun fixSeekableWebm(elementList: List<MatroskaElement>): ByteArray {

        /** SimpleBlock と Timestamp から動画の時間を出す */
        fun getVideoDuration(): Int {
            val lastTimeStamp = elementList.filterId(SpecMatroskaId.Timestamp).last().data.toInt()
            // Cluster は 2,3 バイト目が相対時間になる
            val lastRelativeTime = elementList.filterId(SpecMatroskaId.SimpleBlock).last().data.copyOfRange(1, 3).toInt()
            return lastTimeStamp + lastRelativeTime
        }

        /** 映像トラックの番号を取得 */
        fun getVideoTrackNumber(): Int {
            var videoTrackIndex = -1
            var latestTrackNumber = -1
            var latestTrackType = -1
            for (element in elementList) {
                if (element.matroskaId == SpecMatroskaId.TrackNumber) {
                    latestTrackNumber = element.data.toInt()
                }
                if (element.matroskaId == SpecMatroskaId.TrackType) {
                    latestTrackType = element.data.toInt()
                }
                if (latestTrackType != -1 && latestTrackNumber != -1) {
                    if (latestTrackType == 1) {
                        videoTrackIndex = latestTrackNumber
                        break
                    }
                }
            }
            return videoTrackIndex
        }

        /**
         * Cluster を見て [SpecMatroskaId.CuePoint] を作成する
         *
         * @param clusterStartPosition Cluster 開始位置
         * @return CuePoint
         */
        fun createCuePoint(clusterStartPosition: Int): List<MatroskaElement> {
            // Cluster を上から見ていって CuePoint を作る
            val cuePointList = mutableListOf<MatroskaElement>()
            // 前回追加した Cluster の位置
            var prevPosition = clusterStartPosition
            elementList.forEachIndexed { index, element ->
                if (element.matroskaId == SpecMatroskaId.Cluster) {
                    // Cluster の子から時間を取り出して Cue で使う
                    var childIndex = index
                    var latestTimestamp = -1
                    var latestSimpleBlockRelativeTime = -1
                    while (true) {
                        val childElement = elementList[childIndex++]
                        // Cluster のあとにある Timestamp を控える
                        if (childElement.matroskaId == SpecMatroskaId.Timestamp) {
                            latestTimestamp = childElement.data.toInt()
                        }
                        // Cluster から見た相対時間
                        if (childElement.matroskaId == SpecMatroskaId.SimpleBlock) {
                            latestSimpleBlockRelativeTime = childElement.data.copyOfRange(1, 3).toInt()
                        }
                        // Cluster の位置と時間がわかったので
                        if (latestTimestamp != -1 && latestSimpleBlockRelativeTime != -1) {
                            cuePointList += MatroskaElement(
                                SpecMatroskaId.CuePoint,
                                byteArrayOf(
                                    *MatroskaElement(SpecMatroskaId.CueTime, (latestTimestamp + latestSimpleBlockRelativeTime).toByteArray()).toEbmlByteArray(),
                                    *MatroskaElement(
                                        SpecMatroskaId.CueTrackPositions,
                                        byteArrayOf(
                                            *MatroskaElement(SpecMatroskaId.CueTrack, getVideoTrackNumber().toByteArray()).toEbmlByteArray(),
                                            *MatroskaElement(SpecMatroskaId.CueClusterPosition, prevPosition.toByteArray()).toEbmlByteArray()
                                        )
                                    ).toEbmlByteArray()
                                )
                            )
                            break
                        }
                    }
                    // 進める
                    prevPosition += element.toEbmlByteArray().size
                }
            }
            return cuePointList
        }

        /** SeekHead を組み立てる */
        fun reclusiveCreateSeekHead(
            infoByteArraySize: Int,
            tracksByteArraySize: Int,
            clusterByteArraySize: Int
        ): MatroskaElement {

            /**
             * SeekHead を作成する
             * 注意しないといけないのは、SeekHead に書き込んだ各要素の位置は、SeekHead 自身のサイズを含めた位置にする必要があります。
             * なので、SeekHead のサイズが変わった場合、この後の Info Tracks の位置もその分だけズレていくので、注意が必要。
             */
            fun createSeekHead(seekHeadSize: Int): MatroskaElement {
                val infoPosition = seekHeadSize
                val tracksPosition = infoPosition + infoByteArraySize
                val clusterPosition = tracksPosition + tracksByteArraySize
                // Cue は最後
                val cuePosition = clusterPosition + clusterByteArraySize
                // トップレベル要素、この子たちの位置を入れる
                val topLevelElementList = listOf(
                    SpecMatroskaId.Info to infoPosition,
                    SpecMatroskaId.Tracks to tracksPosition,
                    SpecMatroskaId.Cluster to clusterPosition,
                    SpecMatroskaId.Cues to cuePosition
                ).map { (tag, position) ->
                    MatroskaElement(
                        SpecMatroskaId.Seek,
                        byteArrayOf(
                            *MatroskaElement(SpecMatroskaId.SeekID, tag.idByteArray).toEbmlByteArray(),
                            *MatroskaElement(SpecMatroskaId.SeekPosition, position.toByteArray()).toEbmlByteArray()
                        )
                    )
                }
                val seekHead = MatroskaElement(SpecMatroskaId.SeekHead, topLevelElementList.map { it.toEbmlByteArray() }.concatByteArray())
                return seekHead
            }

            // まず一回 SeekHead 自身のサイズを含めない SeekHead を作る。
            // これで SeekHead 自身のサイズが求められるので、SeekHead 自身を考慮した SeekHead を作成できる。
            var prevSeekHeadSize = createSeekHead(0).toEbmlByteArray().size
            var seekHead: MatroskaElement
            while (true) {
                seekHead = createSeekHead(prevSeekHeadSize)
                val seekHeadSize = seekHead.toEbmlByteArray().size
                // サイズが同じになるまで SeekHead を作り直す
                if (prevSeekHeadSize == seekHeadSize) {
                    break
                } else {
                    prevSeekHeadSize = seekHeadSize
                }
            }

            return seekHead
        }

        // Duration 要素を作る
        val durationElement = MatroskaElement(SpecMatroskaId.Duration, getVideoDuration().toFloat().toBits().toByteArray())
        // Duration を追加した Info を作る
        val infoElement = elementList.filterId(SpecMatroskaId.Info).first().let { before ->
            before.copy(data = before.data + durationElement.toEbmlByteArray())
        }

        // ByteArray にしてサイズが分かるように
        val infoByteArray = infoElement.toEbmlByteArray()
        val tracksByteArray = elementList.filterId(SpecMatroskaId.Tracks).first().toEbmlByteArray()
        val clusterByteArray = elementList.filterId(SpecMatroskaId.Cluster).map { it.toEbmlByteArray() }.concatByteArray()
        // SeekHead を作る
        val seekHeadByteArray = reclusiveCreateSeekHead(
            infoByteArraySize = infoByteArray.size,
            tracksByteArraySize = tracksByteArray.size,
            clusterByteArraySize = clusterByteArray.size
        ).toEbmlByteArray()

        // Cues を作る
        val cuePointList = createCuePoint(seekHeadByteArray.size + infoByteArray.size + tracksByteArray.size)
        val cuesByteArray = MatroskaElement(SpecMatroskaId.Cues, cuePointList.map { it.toEbmlByteArray() }.concatByteArray()).toEbmlByteArray()

        // Segment 要素に書き込むファイルが完成
        // 全部作り直しになる副産物として DataSize が不定長ではなくなります。
        val segment = MatroskaElement(
            SpecMatroskaId.Segment,
            byteArrayOf(
                *seekHeadByteArray,
                *infoByteArray,
                *tracksByteArray,
                *clusterByteArray,
                *cuesByteArray
            )
        )

        // シークできるように修正した WebM のバイナリ
        // WebM 先頭の EBML を忘れずに、これは書き換える必要ないのでそのまま
        return byteArrayOf(
            *elementList.filterId(SpecMatroskaId.EBML).first().toEbmlByteArray(),
            *segment.toEbmlByteArray()
        )
    }
}