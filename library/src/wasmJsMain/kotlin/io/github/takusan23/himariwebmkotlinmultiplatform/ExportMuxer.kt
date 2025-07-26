package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.extension.AUDIO_CODEC_OPUS
import io.github.takusan23.himariwebmkotlinmultiplatform.extension.VIDEO_CODEC_VP9

@OptIn(ExperimentalJsExport::class)
@JsExport
fun createMuxerWebm(): JsReference<HimariWebmBuilder> {
    return HimariWebmBuilder().toJsReference()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun setAudioTrack(
    muxerRef: JsReference<HimariWebmBuilder>,
    samplingRateFloat: Float,
    channelCount: Int
) {
    muxerRef.get().setAudioTrack(AUDIO_CODEC_OPUS, samplingRateFloat, channelCount)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun setVideoTrack(
    muxerRef: JsReference<HimariWebmBuilder>,
    videoWidth: Int,
    videoHeight: Int
) {
    muxerRef.get().setVideoTrack(VIDEO_CODEC_VP9, videoWidth, videoHeight)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun writeAudioTrack(
    muxerRef: JsReference<HimariWebmBuilder>,
    encodeData: JsArray<JsNumber>,
    durationMs: Int,
    isKeyFrame: Boolean,
) {
    muxerRef.get().writeAudioTrack(encodeData.toKotlin(), durationMs.toLong(), isKeyFrame)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun writeVideoTrack(
    muxerRef: JsReference<HimariWebmBuilder>,
    encodeData: JsArray<JsNumber>,
    durationMs: Int,
    isKeyFrame: Boolean,
    _unuse:Boolean // writeAudioTrack とシグネチャが競合する
) {
    muxerRef.get().writeVideoTrack(encodeData.toKotlin(), durationMs.toLong(), isKeyFrame)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun muxerBuild(muxerRef: JsReference<HimariWebmBuilder>): JsArray<JsNumber> {
    return muxerRef.get().build().toWasm()
}
