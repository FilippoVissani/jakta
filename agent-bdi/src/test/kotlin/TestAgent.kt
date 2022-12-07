import io.github.anitvam.agents.bdi.Agent
import io.github.anitvam.agents.bdi.Dispatcher
import io.github.anitvam.agents.bdi.events.Event
import io.github.anitvam.agents.bdi.goals.Achieve
import io.github.anitvam.agents.bdi.goals.ActInternally
import io.github.anitvam.agents.bdi.goals.actions.InternalAction
import io.github.anitvam.agents.bdi.goals.actions.InternalActions
import io.github.anitvam.agents.bdi.goals.actions.InternalRequest
import io.github.anitvam.agents.bdi.plans.Plan
import io.github.anitvam.agents.bdi.plans.PlanLibrary
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Substitution
import it.unibo.tuprolog.core.Var

class TestAgent : DescribeSpec({

    val TEST = object : InternalAction("test", 2) {
        override fun InternalRequest.action() {
            if (arguments[0].isAtom && arguments[1].isAtom) {
                val first: Int = Integer.parseInt(arguments[0].castToAtom().value)
                val second: Int = Integer.parseInt(arguments[1].castToAtom().value)
                println(first)
                println(second)
                System.out.flush()
                first shouldBe second
            } else {
                fail("Wrong Arguments for test action")
            }
        }
    }
    val FAILTEST = object : InternalAction("failtest", 0) {
        override fun InternalRequest.action() {
            fail("This action should not be executed by the agent")
        }
    }
    val PASSTEST = object : InternalAction("passtest", 0) {
        override fun InternalRequest.action() {
            println("This should be shown")
        }
    }
    val start = Atom.of("start")
    val agent = Agent.of(
        events = listOf(Event.ofAchievementGoalInvocation(Achieve(start))),
        internalActions = InternalActions.default() +
            (TEST.signature.name to TEST) +
            (FAILTEST.signature.name to FAILTEST) +
            (PASSTEST.signature.name to PASSTEST),
    )

    describe("An agent") {
        it("should call an internal action specifying its name") {
            val newAgent = agent.copy(
                planLibrary = PlanLibrary.of(
                    Plan.ofAchievementGoalInvocation(
                        value = start,
                        goals = listOf(ActInternally(Struct.of("test", Atom.of("5"), Atom.of("5")))),
                    )
                )
            )
            Dispatcher.threadOf(newAgent).run()
        }
        it("can declare custom actions to be performed") {
            val INCREMENT = object : InternalAction("increment", 2) {
                override fun InternalRequest.action() {
                    val first: Atom = arguments.first().castToAtom()
                    val second: Var = arguments[1].castToVar()
                    val computation = Atom.of((Integer.parseInt(first.value) + 1).toString())
                    result = Substitution.of(second to computation)
                }
            }
            val X = Var.of("X")
            val newAgent = agent.copy(
                planLibrary = PlanLibrary.of(
                    Plan.ofAchievementGoalInvocation(
                        value = start,
                        goals = listOf(
                            ActInternally(Struct.of("increment", Atom.of("5"), X)),
                            ActInternally(Struct.of("test", X, Atom.of("6"))),
                        )
                    )
                ),
                internalActions = agent.context.internalActions + (INCREMENT.signature.name to INCREMENT),
            )
            Dispatcher.threadOf(newAgent).run()
        }
        it("can fail its intention using the internal action fail") {
            val newAgent = agent.copy(
                planLibrary = PlanLibrary.of(
                    Plan.ofAchievementGoalInvocation(
                        value = start,
                        goals = listOf(
                            ActInternally(Struct.of("fail")),
                            ActInternally(Struct.of("failtest")),
                        ),
                    ),
                    Plan.ofAchievementGoalFailure(
                        value = start,
                        goals = listOf(
                            ActInternally(Struct.of("passtest")),
                        ),
                    )
                ),
            )
            Dispatcher.threadOf(newAgent).run()
        }
        it("fails its intention if the action to execute is not found") {
            val newAgent = agent.copy(
                planLibrary = PlanLibrary.of(
                    Plan.ofAchievementGoalInvocation(
                        value = start,
                        goals = listOf(
                            ActInternally(Struct.of("nonexistingaction")),
                            ActInternally(Struct.of("failtest")),
                        ),
                    ),
                    Plan.ofAchievementGoalFailure(
                        value = start,
                        goals = listOf(
                            ActInternally(Struct.of("passtest"))
                        )
                    )
                ),
            )
            try {
                Dispatcher.threadOf(newAgent).run().get()
            } catch (e: Exception) {
                e.printStackTrace()
                fail(e.toString())
            }
        }
    }
})
