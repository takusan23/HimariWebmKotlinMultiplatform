package io.github.takusan23.himariwebmkotlinmultiplatform

@OptIn(ExperimentalJsExport::class)
@JsExport
fun fixSeekableWebm(webmByteArray: JsArray<JsNumber>): JsArray<JsNumber> {
    val kotlinByteArray: ByteArray = webmByteArray.toList().map { it.toInt().toByte() }.toByteArray()
    val elementList = HimariWebmParser.parseWebmLowLevel(kotlinByteArray)
    val fixedByteArray = HimariFixSeekableWebm.fixSeekableWebm(elementList)
    val wasmFixedByteArray: JsArray<JsNumber> = fixedByteArray.map { it.toInt().toJsNumber() }.toJsArray()
    return wasmFixedByteArray
}