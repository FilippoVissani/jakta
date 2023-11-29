package it.unibo.jakta.agents.distributed.broker.plugins

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import it.unibo.jakta.agents.distributed.broker.model.Error
import it.unibo.jakta.agents.distributed.broker.model.SubscriptionManager
import kotlinx.serialization.json.Json
import java.time.Duration

const val PERIOD: Long = 15

fun Application.configureWebSockets(subscriptionManager: SubscriptionManager) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(PERIOD)
        timeout = Duration.ofSeconds(PERIOD)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        webSocket("/publish/{topic}") {
            val topic = call.parameters["topic"] ?: ""
            subscriptionManager.addPublisher(this, topic)
            websocketLogic(call, {
                for (frame in incoming) {
                    subscriptionManager.subscribers(topic)
                        .forEach { it.send(frame.copy()) }
                }
            }, {
                subscriptionManager.removePublisher(this, topic)
            })
        }

        webSocket("/subscribe/{topic}") {
            val topic = call.parameters["topic"] ?: ""
            subscriptionManager.addSubscriber(this, topic)
            websocketLogic(call, {
                for (frame in incoming) {
                    this.send(Frame.Text(Error.BAD_REQUEST.toString()))
                }
            }, {
                subscriptionManager.removeSubscriber(this, topic)
                subscriptionManager.subscribers(topic).forEach {
                    it.send(Frame.Text(Error.CLIENT_DISCONNECTED.toString()))
                }
            })
        }

        webSocket("/subscribe-all/{except...}") {
            val except = call.parameters.getAll("except") ?: emptyList()
            subscriptionManager.availableTopics().minus(except.toSet())
                .forEach { subscriptionManager.addSubscriber(this, it) }
            websocketLogic(call, {
                for (frame in incoming) {
                    this.send(Frame.Text(Error.BAD_REQUEST.toString()))
                }
            }, {
                subscriptionManager.availableTopics().minus(except.toSet())
                    .forEach { subscriptionManager.removeSubscriber(this, it) }
            })
        }
    }
}

private suspend fun websocketLogic(
    call: ApplicationCall,
    tryBlock: suspend () -> Unit,
    finallyBlock: suspend () -> Unit,
) {
    call.application.environment.log.info(
        "New connection: ${call.request.origin.remoteAddress}:${call.request.origin.remotePort}",
    )
    try {
        tryBlock()
    } catch (e: Throwable) {
        call.application.environment.log.error(e.message)
    } finally {
        finallyBlock()
        call.application.environment.log.info(
            "Removing ${call.request.origin.remoteAddress}:${call.request.origin.remotePort}",
        )
    }
}
