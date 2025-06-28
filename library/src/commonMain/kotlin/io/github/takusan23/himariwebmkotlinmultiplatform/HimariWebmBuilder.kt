package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.extension.ASCII_HIMARIWEBM
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.ASCII_OPUS_HEAD
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.ASCII_WEBM
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.AUDIO_TRACK_TYPE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.SIMPLE_BLOCK_FLAGS_KEYFRAME
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.SIMPLE_BLOCK_FLAGS_NONE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.VIDEO_TRACK_TYPE
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.concatByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.listInListIfTrue
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.padding
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toClusterSize
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toDataSize
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toEbmlByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.EncodeData
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.MatroskaElement
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.SpecMatroskaId

class HimariWebmBuilder {

    // Cluster 一時保存先
    private var encodeDataList = mutableListOf<EncodeData>()

    private var audioTrack: MatroskaElement? = null
    private var videoTrack: MatroskaElement? = null

    private var trackCount = 1
    private var audioTrackIndex = -1
    private var videoTrackIndex = -1

    fun setAudioTrack(
        audioCodec: ByteArray,
        audioSamplingRate: Float,
        audioChannelCount: Int
    ) {
        audioTrackIndex = trackCount++
        audioTrack = MatroskaElement(
            SpecMatroskaId.TrackEntry,
            byteArrayOf(
                *MatroskaElement(SpecMatroskaId.TrackNumber, audioTrackIndex.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.TrackUID, audioTrackIndex.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.TrackType, AUDIO_TRACK_TYPE.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.CodecID, audioCodec).toEbmlByteArray(),
                // Opus Codec Private
                // https://wiki.xiph.org/OggOpus#ID_Header
                *MatroskaElement(
                    SpecMatroskaId.CodecPrivate,
                    byteArrayOf(
                        *ASCII_OPUS_HEAD,
                        1.toByte(),
                        audioChannelCount.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        // リトルエンディアンなので逆に
                        *audioSamplingRate.toInt().toByteArray().reversed().toByteArray(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte()
                    )
                ).toEbmlByteArray(),
                *MatroskaElement(
                    SpecMatroskaId.AudioTrack,
                    byteArrayOf(
                        *MatroskaElement(SpecMatroskaId.SamplingFrequency, audioSamplingRate.toBits().toByteArray()).toEbmlByteArray(),
                        *MatroskaElement(SpecMatroskaId.Channels, audioChannelCount.toByteArray()).toEbmlByteArray()
                    )
                ).toEbmlByteArray()
            )
        )
    }

    fun setVideoTrack(
        videoCodec: ByteArray,
        videoWidth: Int,
        videoHeight: Int
    ) {
        videoTrackIndex = trackCount++
        videoTrack = MatroskaElement(
            SpecMatroskaId.TrackEntry,
            byteArrayOf(
                *MatroskaElement(SpecMatroskaId.TrackNumber, videoTrackIndex.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.TrackUID, videoTrackIndex.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.TrackType, VIDEO_TRACK_TYPE.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.CodecID, videoCodec).toEbmlByteArray(),
                *MatroskaElement(
                    SpecMatroskaId.VideoTrack,
                    byteArrayOf(
                        *MatroskaElement(SpecMatroskaId.PixelWidth, videoWidth.toByteArray()).toEbmlByteArray(),
                        *MatroskaElement(SpecMatroskaId.PixelHeight, videoHeight.toByteArray()).toEbmlByteArray()
                    )
                ).toEbmlByteArray()
            )
        )
    }

    fun writeAudioTrack(
        byteArray: ByteArray,
        durationMs: Long,
        isKeyFrame: Boolean = true,
    ) = writeSample(audioTrackIndex, byteArray, durationMs, isKeyFrame)

    fun writeVideoTrack(
        byteArray: ByteArray,
        durationMs: Long,
        isKeyFrame: Boolean
    ) = writeSample(videoTrackIndex, byteArray, durationMs, isKeyFrame)

    fun build(): ByteArray {
        // stop するまで時間がわからない。ようやく分かる
        val duration = encodeDataList.maxOf { it.time }

        // EBMLヘッダー を書き込む
        val ebmlByteArray = MatroskaElement(
            SpecMatroskaId.EBML,
            byteArrayOf(
                *MatroskaElement(SpecMatroskaId.EBMLVersion, 1.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.EBMLReadVersion, 1.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.EBMLMaxIDLength, 4.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.EBMLMaxSizeLength, 8.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.DocType, ASCII_WEBM).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.DocTypeVersion, 4.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.DocTypeReadVersion, 2.toByteArray()).toEbmlByteArray()
            )
        ).toEbmlByteArray()

        // すでに確定している要素は ByteArray にする
        val infoByteArray = MatroskaElement(
            SpecMatroskaId.Info,
            byteArrayOf(
                *MatroskaElement(SpecMatroskaId.TimestampScale, 1_000_000.toByteArray()).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.MuxingApp, ASCII_HIMARIWEBM).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.WritingApp, ASCII_HIMARIWEBM).toEbmlByteArray(),
                *MatroskaElement(SpecMatroskaId.Duration, duration.toFloat().toBits().toByteArray()).toEbmlByteArray()
            )
        ).toEbmlByteArray()
        val tracksByteArray = MatroskaElement(
            SpecMatroskaId.Tracks,
            byteArrayOf(
                *(audioTrack?.toEbmlByteArray() ?: byteArrayOf()),
                *(videoTrack?.toEbmlByteArray() ?: byteArrayOf())
            )
        ).toEbmlByteArray()

        // Cluster をつくる
        // 時系列順に並び替える。音声と映像の並びが追加順ではなく、追加時間順になるように
        encodeDataList.sortBy { it.time }

        // Cluster に入れる SimpleBlock も、わかりやすいように二重の配列にする
        // [ [Data, Data ...], [Data, Data ...] ]
        // つまりこの配列の数だけ Cluster を作り、二重になってる List の中身を Cluster に入れれば良い
        // キーフレームが来たら Cluster にする
        val clusterSimpleBlockChildrenList = encodeDataList.listInListIfTrue { it.isKeyFrame }

        // Cluster を入れる前に、Cluster 全体の大きさと、Cue を生成する
        // Cluster 全体の大きさは Cluster + Timestamp + SimpleBlock ... の全部を足さないといけない
        val allClusterByteSize = clusterSimpleBlockChildrenList.sumOf { children -> children.toClusterSize() }

        // SeekHead を作る
        // SeekHead は自分自身の要素の長さを加味した Info / Tracks の位置を出す必要が有るため大変
        // Cue は一番最後って約束で
        val seekHead = reclusiveCreateSeekHead(
            infoSize = infoByteArray.size,
            tracksSize = tracksByteArray.size,
            clusterSize = allClusterByteSize
        ).toEbmlByteArray()

        // Cue を作る。プレイヤーがシークする際に読み出す位置を教えてあげる
        // これがないとプレイヤーはシークする際に Cluster を上から舐める（かランダムアクセス）が必要になる。
        // プレイヤーによっては Info の Duration があればランダムアクセスでシークできる実装もあるが、Cue に頼っているプレイヤーもある。
        // 初回 Cluster の追加位置
        var prevInsertClusterPosition = seekHead.size + infoByteArray.size + tracksByteArray.size
        val cuesByteArray = MatroskaElement(
            SpecMatroskaId.Cues, clusterSimpleBlockChildrenList.map { children ->
                val encodeData = children.first()
                val seekPosition = encodeData.time.toInt()

                val cuePointByteArray = MatroskaElement(
                    SpecMatroskaId.CuePoint,
                    byteArrayOf(
                        *MatroskaElement(SpecMatroskaId.CueTime, seekPosition.toByteArray()).toEbmlByteArray(),
                        *MatroskaElement(
                            SpecMatroskaId.CueTrackPositions,
                            byteArrayOf(
                                *MatroskaElement(SpecMatroskaId.CueTrack, videoTrackIndex.toByteArray()).toEbmlByteArray(),
                                *MatroskaElement(SpecMatroskaId.CueClusterPosition, prevInsertClusterPosition.toByteArray()).toEbmlByteArray()
                            )
                        ).toEbmlByteArray()
                    )
                ).toEbmlByteArray()

                // 次の Cluster 追加位置を計算する
                prevInsertClusterPosition += children.toClusterSize()

                cuePointByteArray
            }.concatByteArray()
        ).toEbmlByteArray()

        // 書き込む
        return buildList {

            // EBML ヘッダー
            this += ebmlByteArray

            // Segment を書き込む。まずは Id と DataSize
            val segmentIdAndDataSize = byteArrayOf(
                // Segment ID
                *SpecMatroskaId.Segment.idByteArray,
                // DataSize
                *(seekHead.size + infoByteArray.size + tracksByteArray.size + allClusterByteSize + cuesByteArray.size).toDataSize()
            )
            this += segmentIdAndDataSize

            // SeekHead Info Tracks を書き込む
            this += seekHead
            this += infoByteArray
            this += tracksByteArray

            clusterSimpleBlockChildrenList.forEach { children ->

                // 時間
                val timestamp = children.first().time

                // Cluster を作る
                // ここも OOM なりそうでちょい怖いけど 4 秒ごとにエンコード済みデータをメモリに乗せるだけだし大丈夫やろ
                val cluster = MatroskaElement(
                    SpecMatroskaId.Cluster,
                    byteArrayOf(
                        // Cluster 最初の要素は Timestamp
                        *MatroskaElement(SpecMatroskaId.Timestamp, timestamp.toInt().toByteArray()).toEbmlByteArray(),

                        // そしたらあとは SimpleBlock で埋める
                        *children.map { encodeData ->
                            val encodeByteArray = encodeData.encodeData
                            // 先頭にトラック番号、Timestamp からの相対時間（2バイト）、キーフレームかどうかを入れる
                            val trackIndexByteArray = encodeData.trackIndex.toDataSize()
                            val relativeTimeByteArray = (encodeData.time - timestamp).toInt().toByteArray().padding(2)
                            val keyFrameByteArray = (if (encodeData.isKeyFrame) SIMPLE_BLOCK_FLAGS_KEYFRAME else SIMPLE_BLOCK_FLAGS_NONE).toByteArray()

                            MatroskaElement(
                                SpecMatroskaId.SimpleBlock,
                                byteArrayOf(
                                    *trackIndexByteArray,
                                    *relativeTimeByteArray,
                                    *keyFrameByteArray,
                                    *encodeByteArray
                                )
                            ).toEbmlByteArray()
                        }.concatByteArray()
                    )
                ).toEbmlByteArray()

                // 書き込む
                this += cluster
            }

            // Cue は最後
            this += cuesByteArray
        }.concatByteArray()
    }

    private fun writeSample(
        trackIndex: Int,
        byteArray: ByteArray,
        durationMs: Long,
        isKeyFrame: Boolean
    ) {
        encodeDataList += EncodeData(
            trackIndex = trackIndex,
            time = durationMs,
            isKeyFrame = isKeyFrame,
            encodeDataSize = byteArray.size,
            encodeData = byteArray
        )
    }

    /** SeekHead を組み立てる */
    private fun reclusiveCreateSeekHead(
        infoSize: Int,
        tracksSize: Int,
        clusterSize: Int
    ): MatroskaElement {
        // SeekHead を作る・・・・がこれがとても複雑で、こんな感じに書き込みたいとして
        // +---------+----------+------+ ... +-----+
        // | Segment | SeekHead | Info | ... | Cue |
        // +---------+----------+------+ ... +-----+
        //          ↑ Segment の終わりから各要素の位置を SeekHead に書く必要がある。
        //
        // で、これの問題なんですが、
        // SeekHead を書き込む際に各要素の位置を書き込むのですが（Info Tracks など）、
        // この位置計算が、Segment から後なので、当然 SeekHead が書き込まれることを想定したサイズにする必要があります。
        // +---------+------------ ... -+------+
        // | Segment | SeekHead         | Info |
        // +---------+------------ ... -+------+
        //                             ↑ SeekHead 書き込みで Info の位置を知りたいけど、SeekHead 自体もまだ決まっていないので難しい
        // SeekHead + Info + Tracks + Cluster... + Cue の順番で書き込む予定
        // でその SeekHead のサイズが分からないというわけ
        fun createSeekHead(seekHeadSize: Int): MatroskaElement {
            val infoPosition = seekHeadSize
            val tracksPosition = infoPosition + infoSize
            val clusterPosition = tracksPosition + tracksSize
            // Cue は最後
            val cuePosition = clusterPosition + clusterSize
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
        // もちろん SeekHead 自身のサイズを含めた計算をしないとずれるが、
        // それを修正するために再帰的に呼び出し修正する
        // （再帰的に呼び出しが必要な理由ですが、位置の更新で DataSize も拡張が必要になると、後続の位置が全部ずれるので）
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
}