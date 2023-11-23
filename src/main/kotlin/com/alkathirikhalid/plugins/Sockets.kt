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
                thisConnection.name = usernameFrame.readText().replace("\\s".toRegex(), "")

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

                    if (receivedText.startsWith("/whisper ")) {
                        // Handle /whisper command
                        val parts = receivedText.split(" ", limit = 3)
                        if (parts.size == 3) {
                            val targetUsername = parts[1]
                            val message = parts[2]
                            sendWhisper(thisConnection, targetUsername, message, connections)
                        } else {
                            thisConnection.session.send("Invalid /whisper command format.")
                        }
                    } else {
                        // Broadcast the message to all users
                        connections.forEach {
                            it?.session?.send("[${thisConnection.name}]: $receivedText")
                        }
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

private suspend fun sendWhisper(
    sender: Connection,
    targetUsername: String,
    message: String,
    connections: Set<Connection?>
) {
    if (targetUsername.equals("anonymous", ignoreCase = true)) {
        // Throw an error if whispering to "anonymous" ignore case
        sender.session.send("Invalid whisper target name: anonymous")
        return
    }
    val targetConnection = connections.firstOrNull { it?.name == targetUsername }
    if (targetConnection != null) {
        // Send a private message to the target user
        targetConnection.session.send("[Whisper from ${sender.name}]: $message")
    } else {
        // Inform the sender that the target user was not found
        sender.session.send("User $targetUsername not found.")
    }
}