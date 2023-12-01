package it.unibo.jakta.agents.distributed.broadcast

import it.unibo.jakta.agents.bdi.Agent
import it.unibo.jakta.agents.bdi.Jakta
import it.unibo.jakta.agents.bdi.beliefs.Belief
import it.unibo.jakta.agents.bdi.beliefs.BeliefBase
import it.unibo.jakta.agents.bdi.environment.Environment
import it.unibo.jakta.agents.bdi.events.Event
import it.unibo.jakta.agents.bdi.executionstrategies.ExecutionStrategy
import it.unibo.jakta.agents.bdi.goals.Achieve
import it.unibo.jakta.agents.bdi.goals.ActInternally
import it.unibo.jakta.agents.bdi.goals.RemoveBelief
import it.unibo.jakta.agents.bdi.goals.UpdateBelief
import it.unibo.jakta.agents.bdi.plans.Plan
import it.unibo.jakta.agents.bdi.plans.PlanLibrary
import it.unibo.jakta.agents.distributed.RemoteService
import it.unibo.jakta.agents.distributed.dmas.DMas

fun main() {
    val env = Environment.of(
        externalActions = mapOf(
            sendAction.signature.name to sendAction,
        ),
    )

    val pinger = Agent.of(
        name = "pingerBroadcast",
        beliefBase = BeliefBase.of(
            Belief.fromSelfSource(Jakta.parseStruct("turn(me)")),
            Belief.fromSelfSource(Jakta.parseStruct("other(pongerBroadcast)")),
        ),
        events = listOf(Event.ofAchievementGoalInvocation(Achieve.of(Jakta.parseStruct("send_ping")))),

        planLibrary = PlanLibrary.of(
            Plan.ofAchievementGoalInvocation(
                value = Jakta.parseStruct("send_ping"),
                guard = Jakta.parseStruct("turn(source(self), me) & other(source(self), Receiver)"),
                goals = listOf(
                    UpdateBelief.of(Belief.fromSelfSource(Jakta.parseStruct("turn(other)"))),
                    Achieve.of(Jakta.parseStruct("broadcast(ball)")),
                ),
            ),
            Plan.ofBeliefBaseAddition(
                guard = Jakta.parseStruct("turn(source(self), other) & other(source(self), Sender)"),
                belief = Belief.from(Jakta.parseStruct("ball(source(Sender))")),
                goals = listOf(
                    UpdateBelief.of(Belief.fromSelfSource(Jakta.parseStruct("turn(me)"))),
                    ActInternally.of(Jakta.parseStruct("print(\"Received ball from\", Sender)")),
                    RemoveBelief.of(Belief.from(Jakta.parseStruct("ball(source(Sender))"))),
                    ActInternally.of(Jakta.parseStruct("print(\"Pinger hasDone\")")),
                ),
            ),
            it.unibo.jakta.agents.distributed.pingpong.sendPlan,
        ),
    )

    val ponger = RemoteService("pongerBroadcast")

    val dmas = DMas.withEmbeddedBroker(
        ExecutionStrategy.oneThreadPerAgent(),
        env,
        listOf(pinger),
        listOf(ponger),
    )
    dmas.start()
}
