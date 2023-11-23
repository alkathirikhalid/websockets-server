package com.alkathirikhalid

import com.alkathirikhalid.plugins.configureRouting
import com.alkathirikhalid.plugins.configureSockets
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureRouting()
}
