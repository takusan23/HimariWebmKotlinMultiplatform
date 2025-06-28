package io.github.takusan23.himariwebmkotlinmultiplatform.extension

import io.github.takusan23.himariwebmkotlinmultiplatform.tool.EncodeData
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.MatroskaElement
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.MatroskaId
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.SpecMatroskaId
import io.github.takusan23.himariwebmkotlinmultiplatform.tool.UnknownMatroskaId
import kotlin.math.max

/** "webm"の ASCII 表現 */
internal val ASCII_WEBM = byteArrayOf(0x77, 0x65, 0x62, 0x6d)

/** "OpusHead"の ASCII 表現 */
internal val ASCII_OPUS_HEAD = byteArrayOf(0x4f, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64)

/** "himariwebm"の ASCII 表現 */
internal val ASCII_HIMARIWEBM = byteArrayOf(0x68, 0x69, 0x6d, 0x61, 0x72, 0x69, 0x77, 0x65, 0x62, 0x6d)

/** DataSize の長さが不定の場合の表現 */
internal val UNKNOWN_DATA_SIZE = byteArrayOf(0x1F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

/** VINT とバイト数 */
private val BIT_SIZE_LIST = listOf(
    0b1000_0000 to 1,
    0b0100_0000 to 2,
    0b0010_0000 to 3,
    0b0001_0000 to 4,
    0b0000_1000 to 5,
    0b0000_0100 to 6,
    0b0000_0010 to 7,
    0b0000_0001 to 8
)

/** 最大値と必要なバイト数 */
private val INT_TO_BYTEARRAY_LIST = listOf(
    0xFF to 1,
    0xFFFF to 2,
    0xFFFFFF to 3,
    0x7FFFFFFF to 4
)

/** TrackType が Video */
internal const val VIDEO_TRACK_TYPE = 1

/** TrackType が Audio */
internal const val AUDIO_TRACK_TYPE = 2

/** キーフレームなら */
internal const val SIMPLE_BLOCK_FLAGS_KEYFRAME = 0x80

/** キーフレームじゃない */
internal const val SIMPLE_BLOCK_FLAGS_NONE = 0x00

/** [MatroskaElement]の配列から[MatroskaId]をフィルターする */
internal fun List<MatroskaElement>.filterId(matroskaId: MatroskaId) = this.filter { it.matroskaId.idByteArray.contentEquals(matroskaId.idByteArray) }

/**
 * VINT の長さを返す
 * 注意してほしいのは、DataSize は Data の長さを表すものだが、この DataSize 自体の長さが可変長。
 */
internal fun Byte.getElementLength(): Int {
    // UInt にする必要あり
    val uIntValue = toInt().andFF()
    // 左から最初に立ってるビットを探して、サイズをもとめる
    // 予めビットとサイズを用意しておく読みやすさ最優先実装...
    return BIT_SIZE_LIST.first { (bit, _) ->
        // ビットを AND して、0 以外を探す（ビットが立ってない場合は 0 になる）
        uIntValue and bit != 0
    }.second
}

/** [MatroskaElement] を EBML 要素のバイト配列にする */
internal fun MatroskaElement.toEbmlByteArray(): ByteArray {
    // ID
    val idByteArray = this.matroskaId.idByteArray
    // DataSize
    val dataSize = this.data.size
    val dataSizeByteArray = dataSize.toDataSize()
    // Data
    val data = this.data
    // ByteArray にする
    return byteArrayOf(*idByteArray, *dataSizeByteArray, *data)
}

/**
 * [MatroskaId]から EBML 要素のバイト配列のサイズだけ出す。
 * [toEbmlByteArray]を使うとバイト配列が帰ってくるが、サイズだけ欲しいときはこっちを使う。
 *
 * @param dataSize 実際のデータの大きさ。Data とそれ自身のサイズを示す DataSize で使われる
 */
internal fun MatroskaId.toEbmlByteArraySize(dataSize: Int): Int {
    // ID
    val idByteArraySize = this.idByteArray.size
    // DataSize
    val dataSizeByteArraySize = dataSize.toDataSize().size
    // すべて足す
    return idByteArraySize + dataSizeByteArraySize + dataSize
}

/** [ByteArray]から[MatroskaId.SpecMatroskaId]を探す。未知の ID の場合は [MatroskaId.UnknownMatroskaId]。 */
internal fun ByteArray.toMatroskaId(): MatroskaId {
    return SpecMatroskaId.entries.firstOrNull { it.idByteArray.contentEquals(this) } ?: UnknownMatroskaId(this)
}

/** [ByteArray]が入っている[List]を、全部まとめて[ByteArray]にする */
internal fun List<ByteArray>.concatByteArray(): ByteArray {
    val byteArray = ByteArray(this.sumOf { it.size })
    var write = 0
    this.forEach { cluster ->
        repeat(cluster.size) { read ->
            byteArray[write++] = cluster[read]
        }
    }
    return byteArray
}

/**
 * DataSize 要素から実際の Data の大きさを出す
 * @see [getElementLength]
 * @return 長さ不定の場合は -1
 */
internal fun ByteArray.getDataSize(): Int {
    // 例外で、 01 FF FF FF FF FF FF FF のときは長さが不定なので...
    // Segment / Cluster の場合は子要素の長さを全部足せば出せると思うので、、、
    if (contentEquals(UNKNOWN_DATA_SIZE)) {
        return -1
    }

    var firstByte = first().toInt().andFF()
    // DataSize 要素の場合、最初に立ってるビットを消す
    for ((bit, _) in BIT_SIZE_LIST) {
        if (firstByte and bit != 0) {
            // XOR で両方同じ場合は消す
            firstByte = firstByte xor bit
            // 消すビットは最初だけなので
            break
        }
    }
    // 戻す
    return if (size == 1) {
        firstByte
    } else {
        byteArrayOf(firstByte.toByte(), *copyOfRange(1, size)).toInt()
    }
}

/** 実際のデータの大きさから DataSize 要素のバイト配列にする */
internal fun Int.toDataSize(): ByteArray {
    // Int を ByteArray に
    val byteArray = this.toByteArray()
    // 最初のバイトは、DataSize 自体のサイズを入れる必要がある（ VINT ）
    val firstByte = byteArray.first().toInt().andFF()

    // DataSize 自体のサイズとして、最初のバイトに OR で 1 を立てる
    var resultByteArray = byteArrayOf()
    BIT_SIZE_LIST.forEachIndexed { index, (bit, size) ->
        if (size == byteArray.size) {
            resultByteArray = if (firstByte < bit) {
                // V_INT を先頭のバイトに書き込む
                byteArrayOf(
                    (firstByte or bit).toByte(),
                    *byteArray.copyOfRange(1, byteArray.size)
                )
            } else {
                // 最初のバイトに書き込めない場合
                // （DataSize 自身のサイズのビットを立てる前の時点で、最初のバイトのほうが大きい場合）
                // size が +1 するので、配列の添字は -1 する必要あり
                byteArrayOf(BIT_SIZE_LIST[index + 1].first.toByte(), *byteArray)
            }
        }
    }
    // 戻す
    return resultByteArray
}

/** List<tool.EncodeData> を入れた Cluster を作ったときのデータの大きさを出す */
internal fun List<EncodeData>.toClusterSize(): Int {
    // Timestamp を作ってサイズだけ出す
    // children 最初の時間で
    val time = this.first().time.toInt()
    val timestampSize = SpecMatroskaId.Timestamp.toEbmlByteArraySize(time.toByteArray().size)
    // SimpleBlock はサイズだけ
    // SimpleBlock は先頭 4 バイト埋める必要があるため、4 足す
    val allSimpleBlockSize = this.sumOf { SpecMatroskaId.SimpleBlock.toEbmlByteArraySize(it.encodeDataSize + 4) }
    // Cluster
    return SpecMatroskaId.Cluster.toEbmlByteArraySize(timestampSize + allSimpleBlockSize)
}

/** Int を ByteArray に変換する */
internal fun Int.toByteArray(): ByteArray {
    // 多分 max 4 バイト
    val size = INT_TO_BYTEARRAY_LIST.first { (maxValue, _) ->
        this <= maxValue
    }.second
    var l = this
    val result = ByteArray(size)
    for (i in 0..<size) {
        result[i] = (l and 0xff).toByte()
        l = l shr 8
    }
    // 逆になってるのでもどす
    result.reverse()
    return result
}

/** 0x00 で埋める */
internal fun ByteArray.padding(size: Int): ByteArray {
    val byteArray = ByteArray(size)
    val paddingSize = size - this.size
    repeat(paddingSize) { i ->
        byteArray[i] = 0x00
    }
    repeat(this.size) { i ->
        byteArray[paddingSize + i] = this[i]
    }
    return byteArray
}

/** ByteArray から Int へ変換する。ByteArray 内にある Byte は符号なしに変換される。 */
internal fun ByteArray.toInt(): Int {
    // 先頭に 0x00 があれば消す
    val validValuePos = max(0, this.indexOfFirst { it != 0x00.toByte() })
    var result = 0
    // 逆にする
    // これしないと左側にバイトが移動するようなシフト演算？になってしまう
    // for を 多い順 にすればいいけどこっちの方でいいんじゃない
    drop(validValuePos).reversed().also { bytes ->
        for (i in 0 until bytes.count()) {
            result = result or (bytes.get(i).toInt().andFF() shl (8 * i))
        }
    }
    return result
}

/**
 * Segment の長さ不定のデータサイズを上から舐めてもとめる。
 * ByteArray は ID までは読めている、DataSize は長さ不定なので 8 バイト固定。というわけで ID + DataSize を消した Data だけください。
 */
internal fun ByteArray.analyzeUnknownDataSizeForSegmentData(): Int {
    var index = 0

    while (true) {
        // Segment も Cluster も親要素なので、それぞれの子を見ていけばデータの長さが分かるはず。
        val idLength = this[index].getElementLength()
        val idElement = this.copyOfRange(index, index + idLength)
        // ID 分足していく
        index += idLength

        // DataSize を取り出す
        val dataSizeLength = this[index].getElementLength()
        // Cluster の場合は長さ不定の場合があるため、追加処理
        val dataSizeElement = this.copyOfRange(index, index + dataSizeLength).getDataSize().let { dataSizeOrUnknown ->
            // 長さが求まっていればそれを使う
            if (dataSizeOrUnknown != -1) {
                return@let dataSizeOrUnknown
            }

            // Cluster は上から舐めて求められる
            val dataByteArray = this.copyOfRange(index + UNKNOWN_DATA_SIZE.size, this.size)
            if (idElement.toMatroskaId() == SpecMatroskaId.Cluster) {
                // Cluster の長さ不定は求められる、再帰で求める。Data を渡す
                dataByteArray.analyzeUnknownDataSizeForClusterData()
            } else {
                throw RuntimeException("長さ不定")
            }
        }

        // DataSize 分足していく
        index += dataSizeLength
        // データの大きさを出すため、Data の中身までは見ない
        // Data 分足していく
        index += dataSizeElement

        // もう読み出せない場合はここまでをデータの大きさとする
        if (size <= index) {
            break
        }
        // 次の要素が 3 バイト以上ない場合は break（解析できない）
        if (size <= index + 3) {
            break
        }
    }
    return index
}

/**
 * Cluster の長さ不定のデータサイズを上から舐めてもとめる。
 *
 * ByteArray は ID までは読めている、DataSize は長さ不定なので 8 バイト固定。というわけで ID + DataSize を消した Data だけください。
 */
internal fun ByteArray.analyzeUnknownDataSizeForClusterData(): Int {
    var index = 0

    while (true) {
        // Segment も Cluster も親要素なので、それぞれの子を見ていけばデータの長さが分かるはず。
        val idLength = this[index].getElementLength()
        val idElement = this.copyOfRange(index, index + idLength)

        // Cluster だった場合は break。次の Cluster にぶつかったってことです。
        if (idElement.toMatroskaId() == SpecMatroskaId.Cluster) {
            break
        }

        // ID 分足していく
        index += idLength
        // DataSize を取り出す
        val dataSizeLength = this[index].getElementLength()
        val dataSizeElement = this.copyOfRange(index, index + dataSizeLength).getDataSize()
        // DataSize 分足していく
        index += dataSizeLength
        // データの大きさを出すため、Data の中身までは見ない
        // Data 分足していく
        index += dataSizeElement

        // もう読み出せない場合はここまでをデータの大きさとする
        if (size <= index) {
            break
        }
        // もしかしたら他のブラウザでもなるかもしれないけど、
        // Chromeの場合、録画終了を押したとき、要素をきれいに終わらせてくれるわけでは無いらしく SimpleBlock の途中だろうとぶった切ってくるらしい、中途半端にデータが余ることがある
        // 例：タグの A3 で終わるなど
        // その場合にエラーにならないように、この後3バイト（ID / DataSize / Data それぞれ1バイト）ない場合はループを抜ける
        if (size <= index + 3) {
            break
        }
    }
    return index
}

/** ByteをIntに変換した際に、符号付きIntになるので、AND 0xFF するだけの関数 */
internal fun Int.andFF() = this and 0xFF