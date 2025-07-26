package io.github.takusan23.himariwebmkotlinmultiplatform

internal fun JsArray<JsNumber>.toKotlin() = this.toList().map { it.toInt().toByte() }.toByteArray()

internal fun ByteArray.toWasm() = this.map { it.toInt().toJsNumber() }.toJsArray()
