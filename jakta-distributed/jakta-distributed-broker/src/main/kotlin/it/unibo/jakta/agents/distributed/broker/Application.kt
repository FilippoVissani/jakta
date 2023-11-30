package it.unibo.jakta.agents.distributed.broker

import io.ktor.server.application.Application
import io.ktor.websocket.DefaultWebSocketSession
import it.unibo.jakta.agents.distributed.broker.model.SubscriptionManager
import it.unibo.jakta.agents.distributed.broker.plugins.configureRouting
import it.unibo.jakta.agents.distributed.broker.plugins.configureWebSockets

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val subscriptionManager = SubscriptionManager<DefaultWebSocketSession>()
    configureRouting(subscriptionManager)
    configureWebSockets(subscriptionManager)
}
