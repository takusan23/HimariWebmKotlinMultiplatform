package io.github.takusan23.himariwebmkotlinmultiplatform

@OptIn(ExperimentalJsExport::class)
@JsExport
fun fixSeekableWebm(webmByteArray: JsArray<JsNumber>): JsArray<JsNumber> {
    val kotlinByteArray = webmByteArray.toKotlin()
    val elementList = HimariWebmParser.parseWebmLowLevel(kotlinByteArray)
    val fixedByteArray = HimariFixSeekableWebm.fixSeekableWebm(elementList)
    val wasmFixedByteArray: JsArray<JsNumber> = fixedByteArray.toWasm()
    return wasmFixedByteArray
}
