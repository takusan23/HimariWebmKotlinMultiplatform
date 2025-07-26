package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.extension.filterId
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.toByteArray
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.SpecMatroskaId
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParseAndMuxerText {

    @Test
    fun WebMのパースが出来る() {
        // 適当な WebM を作成
        val encodeData = byteArrayOf(0x01, 0x01, 0x01, 0x01)
        val videoWidth = 1280
        val videoHeight = 720
        val videoCodecName = byteArrayOf(0x56, 0x5f, 0x56, 0x50, 0x39) // V_VP9

        val samplingRate = 48_000F
        val channelCount = 2
        val audioCodecName = byteArrayOf(0x41, 0x5f, 0x4f, 0x50, 0x55, 0x53) // O_OPUS


        val builder = HimariWebmBuilder()
        // トラックを指定
        builder.setAudioTrack(audioCodecName, samplingRate, channelCount)
        builder.setVideoTrack(videoCodecName, videoWidth, videoHeight)

        // 書き込む
        builder.writeVideoTrack(byteArray = encodeData, durationMs = 0, isKeyFrame = true)
        builder.writeAudioTrack(byteArray = encodeData, durationMs = 0, isKeyFrame = true)
        (1..10).forEach {
            builder.writeVideoTrack(byteArray = encodeData, durationMs = it * 1_000L, isKeyFrame = false)
            builder.writeAudioTrack(byteArray = encodeData, durationMs = it * 1_000L, isKeyFrame = false)
        }
        // キーフレーム
        builder.writeVideoTrack(byteArray = encodeData, durationMs = 11_000L, isKeyFrame = true)
        builder.writeAudioTrack(byteArray = encodeData, durationMs = 11_000L, isKeyFrame = true)

        // WebM 作成
        val webmByteArray = builder.build()

        // パースしてみる
        val parse = HimariWebmParser.parseWebm(webmByteArray)

        // 合っていること
        assertNotNull(parse.videoTrack)
        assertNotNull(parse.audioTrack)
        assertEquals(parse.videoTrack?.videoWidth, videoWidth)
        assertEquals(parse.videoTrack?.videoHeight, videoHeight)
        assertEquals(parse.audioTrack?.audioSamplingRate, samplingRate)
        assertEquals(parse.audioTrack?.audioChannelCount, channelCount)
        assertContentEquals(parse.videoTrack?.videoCodec, videoCodecName)
        assertContentEquals(parse.audioTrack?.audioCodec, audioCodecName)
        assertEquals(parse.videoEncodeData?.size, 12)
        assertEquals(parse.audioEncodeData?.size, 12)
    }

    @Test
    fun エンコーダーからの値を受け取ってWebMを作成できること() {
        val encodeData = byteArrayOf(0x01, 0x01, 0x01, 0x01)

        val videoWidth = 1280
        val videoHeight = 720
        val videoCodecName = byteArrayOf(0x56, 0x5f, 0x56, 0x50, 0x39) // V_VP9

        val samplingRate = 48_000F
        val channelCount = 2
        val audioCodecName = byteArrayOf(0x41, 0x5f, 0x4f, 0x50, 0x55, 0x53) // O_OPUS


        val builder = HimariWebmBuilder()
        // トラックを指定
        builder.setAudioTrack(audioCodecName, samplingRate, channelCount)
        builder.setVideoTrack(videoCodecName, videoWidth, videoHeight)

        // 書き込む
        repeat(10) {
            builder.writeVideoTrack(byteArray = encodeData, durationMs = it * 1_000L, isKeyFrame = false)
            builder.writeAudioTrack(byteArray = encodeData, durationMs = it * 1_000L, isKeyFrame = false)
        }
        // キーフレーム
        builder.writeVideoTrack(byteArray = encodeData, durationMs = 11_000L, isKeyFrame = true)
        builder.writeAudioTrack(byteArray = encodeData, durationMs = 11_000L, isKeyFrame = true)

        // WebM 作成
        val webmByteArray = builder.build()

        // パースしてみる
        val parse = HimariWebmParser.parseWebmLowLevel(webmByteArray)

        // 合っていること
        assertEquals(parse.filterId(SpecMatroskaId.TrackType).size, 2)
        assertTrue { parse.filterId(SpecMatroskaId.CodecID).any { it.data.contentEquals(videoCodecName) } }
        assertTrue { parse.filterId(SpecMatroskaId.CodecID).any { it.data.contentEquals(audioCodecName) } }
        assertContentEquals(parse.filterId(SpecMatroskaId.PixelWidth).first().data, videoWidth.toByteArray())
        assertContentEquals(parse.filterId(SpecMatroskaId.PixelHeight).first().data, videoHeight.toByteArray())
        assertContentEquals(parse.filterId(SpecMatroskaId.SamplingFrequency).first().data, samplingRate.toBits().toByteArray())
        assertContentEquals(parse.filterId(SpecMatroskaId.Channels).first().data, channelCount.toByteArray())
    }


}
