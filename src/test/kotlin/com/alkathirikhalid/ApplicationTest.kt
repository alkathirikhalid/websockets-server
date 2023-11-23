package com.alkathirikhalid

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testConversation() {
        testApplication {
            val client = createClient {
                install(WebSockets)
            }

            client.webSocket("/chat") {
                val nameRequestText = (incoming.receive() as? Frame.Text)?.readText() ?: ""
                println(nameRequestText)
                assertEquals("Please enter your name:", nameRequestText)

                val name = "alkathirikhalid"
                val message = "Hello"

                send(Frame.Text(name))
                send(Frame.Text(message))

                val responseText = (incoming.receive() as Frame.Text).readText()
                assertEquals("[$name]: $message", responseText)
            }
        }
    }
}