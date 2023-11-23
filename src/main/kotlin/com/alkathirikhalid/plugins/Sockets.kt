package com.alkathirikhalid.plugins

import com.alkathirikhalid.Connection
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

        webSocket("/chat") {
            val thisConnection = Connection(this)
            try {
                // Ask the user to enter a username
                send("Please enter your name:")
                val usernameFrame = incoming.receive() as Frame.Text
                thisConnection.name = usernameFrame.readText()

                // Notify everyone about the new user
                connections.forEach {
                    it?.session?.send("[Chat]: ${thisConnection.name} has joined the chat.")
                }

                // Broadcast the number of users inclusive + 1
                connections.forEach {
                    it?.session?.send("[Chat]: There are a total of ${connections.count() + 1} users.")
                }

                // Add the new user to the connections set
                connections += thisConnection

                // Main chat loop
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()

                    // Broadcast the message to all users
                    connections.forEach {
                        it?.session?.send("[${thisConnection.name}]: $receivedText")
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                // Remove the user when they disconnect
                connections -= thisConnection
                connections.forEach {
                    it?.session?.send("[Chat]: ${thisConnection.name} has left the chat.")
                }
            }
        }
    }
}

