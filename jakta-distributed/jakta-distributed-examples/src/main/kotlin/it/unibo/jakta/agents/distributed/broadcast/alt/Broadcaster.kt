package it.unibo.jakta.agents.distributed.broadcast.alt

import it.unibo.jakta.agents.bdi.Agent
import it.unibo.jakta.agents.bdi.Jakta
import it.unibo.jakta.agents.bdi.actions.ExternalRequest
import it.unibo.jakta.agents.bdi.actions.impl.AbstractExternalAction
import it.unibo.jakta.agents.bdi.environment.Environment
import it.unibo.jakta.agents.bdi.events.Event
import it.unibo.jakta.agents.bdi.executionstrategies.ExecutionStrategy
import it.unibo.jakta.agents.bdi.goals.Act
import it.unibo.jakta.agents.bdi.goals.ActInternally
import it.unibo.jakta.agents.bdi.messages.Achieve
import it.unibo.jakta.agents.bdi.messages.Message
import it.unibo.jakta.agents.bdi.messages.Tell
import it.unibo.jakta.agents.bdi.plans.Plan
import it.unibo.jakta.agents.bdi.plans.PlanLibrary
import it.unibo.jakta.agents.distributed.dmas.DMas

fun main() {
    val broadcastAction = object : AbstractExternalAction("broadcast", 2) {
        override fun action(request: ExternalRequest) {
            val type = request.arguments[0].castToAtom()
            val message = request.arguments[1].castToStruct()
            when (type.value) {
                "tell" -> broadcastMessage(Message(request.sender, Tell, message))
                "achieve" -> broadcastMessage(
                    Message(request.sender, Achieve, message),
                )
            }
        }
    }

    val env = Environment.of(
        externalActions = mapOf(
            broadcastAction.signature.name to broadcastAction,
        ),
    )

    val broadcaster = Agent.of(
        name = "broadcaster",
        events = listOf(
            Event.ofAchievementGoalInvocation(
                it.unibo.jakta.agents.bdi.goals.Achieve.of(
                    Jakta.parseStruct("broadcast"),
                ),
            ),
        ),
        planLibrary = PlanLibrary.of(
            Plan.ofAchievementGoalInvocation(
                value = Jakta.parseStruct("broadcast"),
                goals = listOf(
                    ActInternally.of(Jakta.parseStruct("print(\"Broadcast message\")")),
                    Act.of(Jakta.parseStruct("broadcast(tell, greetings)")),
                ),
            ),
        ),
    )

    DMas.fromWebSocketNetwork(
        ExecutionStrategy.oneThreadPerAgent(),
        env,
        listOf(broadcaster),
        emptyList(),
        "localhost",
        8080,
    ).start()
}
