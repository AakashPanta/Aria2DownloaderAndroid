package com.aria2.downloader.domain.engine

object Aria2Runtime {
    const val DEFAULT_RPC_PORT: Int = 6800
    val RPC_PORT_RANGE: IntRange = 6800..6810

    @Volatile
    var rpcPort: Int = DEFAULT_RPC_PORT

    @Volatile
    var lastStartupLog: String = ""

    @Volatile
    var lastStartupError: String = ""
}
