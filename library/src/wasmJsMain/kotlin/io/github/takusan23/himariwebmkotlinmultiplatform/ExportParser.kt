package io.github.takusan23.himariwebmkotlinmultiplatform

import io.github.takusan23.himariwebmkotlinmultiplatform.tool.EncodeData
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.WebmParseResult

@OptIn(ExperimentalJsExport::class)
@JsExport
fun parseWebm(webmByteArray: JsArray<JsNumber>): JsReference<WebmParseResult> {
    return HimariWebmParser.parseWebm(webmByteArray.toKotlin()).toJsReference()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getAudioCodecFromWebmParseResult(reference: JsReference<WebmParseResult>): JsArray<JsNumber>? {
    return reference.get().audioTrack?.audioCodec?.toWasm()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getAudioSamplingRateFromWebmParseResult(reference: JsReference<WebmParseResult>): Float? {
    return reference.get().audioTrack?.audioSamplingRate
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getAudioChannelCountFromWebmParseResult(reference: JsReference<WebmParseResult>): Int? {
    return reference.get().audioTrack?.audioChannelCount
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getVideoCodecFromWebmParseResult(reference: JsReference<WebmParseResult>): JsArray<JsNumber>? {
    return reference.get().videoTrack?.videoCodec?.toWasm()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getVideoWidthFromWebmParseResult(reference: JsReference<WebmParseResult>): Int? {
    return reference.get().videoTrack?.videoWidth
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getVideoHeightFromWebmParseResult(reference: JsReference<WebmParseResult>): Int? {
    return reference.get().videoTrack?.videoHeight
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getAudioEncodeDataFromWebmParseResult(reference: JsReference<WebmParseResult>): JsArray<JsReference<EncodeData>>? {
    return reference.get().audioEncodeData?.map { it.toJsReference() }?.toJsArray()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getVideoEncodeDataFromWebmParseResult(reference: JsReference<WebmParseResult>): JsArray<JsReference<EncodeData>>? {
    return reference.get().videoEncodeData?.map { it.toJsReference() }?.toJsArray()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getTrackIndexFromEncodeData(reference: JsReference<EncodeData>): Int {
    return reference.get().trackIndex
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getTimeFromEncodeData(reference: JsReference<EncodeData>): Int {
    // BigInt になってしまうので、Int にしている
    return reference.get().time.toInt()
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun isKeyFrameFromEncodeData(reference: JsReference<EncodeData>): Boolean {
    return reference.get().isKeyFrame
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getEncodeDataSizeFromEncodeData(reference: JsReference<EncodeData>): Int {
    return reference.get().encodeDataSize
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun getEncodeDataFromEncodeData(reference: JsReference<EncodeData>): JsArray<JsNumber> {
    return reference.get().encodeData.toWasm()
}
