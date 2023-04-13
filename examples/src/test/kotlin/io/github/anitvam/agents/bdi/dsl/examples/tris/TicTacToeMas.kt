package io.github.anitvam.agents.bdi.dsl.examples.tris

import io.github.anitvam.agents.bdi.Message
import io.github.anitvam.agents.bdi.beliefs.BeliefBase
import io.github.anitvam.agents.bdi.dsl.MasScope
import io.github.anitvam.agents.bdi.dsl.beliefs.BeliefsScope
import io.github.anitvam.agents.bdi.dsl.beliefs.fromPercept
import io.github.anitvam.agents.bdi.dsl.beliefs.selfSourced
import io.github.anitvam.agents.bdi.dsl.examples.tris.TicTacToeLiterals.allPossibleCombinationsOf
import io.github.anitvam.agents.bdi.dsl.examples.tris.TicTacToeLiterals.cell
import io.github.anitvam.agents.bdi.dsl.mas
import io.github.anitvam.agents.bdi.messages.Tell
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.dsl.lp

fun BeliefsScope.alignment(name: String, dx: Int, dy: Int) {
    val second = cell(X, Y, Z).fromPercept
    val first = cell(A, B, C).fromPercept
    rule(name(listOf(second)) impliedBy second)
    rule(name(listFrom(first, second, last = W)) .impliedBy(
        first,
        second,
        (X - A) arithEq dx,
        (Y - B) arithEq dy,
        name(listFrom(second, last = W))
    ))

}

fun ticTacToe(n: Int = 3) = mas {
    environment {
        from(GridEnvironment(n))
        actions {
            action(Put)
            action("printGrid", 0) {
                for(row in environment.data["grid"] as Array<*>) {
                    for(cell in row as CharArray) {
                        print("$cell \t")
                    }
                    println()
                }
            }
            action("passTurn", 1) {
                val other: Atom = argument(0)
                println("End of $sender turn, passing to $other")
                this.sendMessage(other.value, Message(this.sender, Tell, Atom.of("endTurn")))
            }
        }
    }
    generatePlayer("x", "o", n)
    generatePlayer("o", "x", n)
}

fun MasScope.generatePlayer(mySymbol: String, otherSymbol: String, gridDimension: Int) = agent("$mySymbol-agent") {
    beliefs {
        alignment("vertical", dx = 0, dy = 1)
        alignment("horizontal", dx = 1, dy = 0)
        alignment("diagonal", dx = 1, dy = 1)
        alignment("antidiagonal", dx = 1, dy = -1)
        for (direction in arrayOf("vertical", "horizontal", "diagonal", "antidiagonal")) {
            rule("aligned"(L) impliedBy direction(L).selfSourced)
        }
        if (mySymbol=="x") fact("turn"("self")) else fact("turn"("other"))
    }
    goals { if (mySymbol=="x") achieve("play") }
    plans {
        arrayOf(mySymbol, otherSymbol).map { symbol ->
            + achieve("play") onlyIf {
                "aligned"((1..gridDimension).map { cell(symbol = symbol).fromPercept }) and "turn"("self").selfSourced
            } then {
                iact("print"(if (symbol == mySymbol) "I won!" else "I lost!"))
                iact("stop")
            }
        }
        for (winningLine in allPossibleCombinationsOf(cell(X, Y, "e").fromPercept, cell(symbol = mySymbol).fromPercept, cell(symbol = "e").fromPercept, gridDimension - 1 )) {
            + achieve("play") onlyIf {
                "aligned"(winningLine).selfSourced and "turn"("self").selfSourced
            } then {
                act("put"(X, Y, mySymbol))
                act("printGrid")
                update("turn"("other").selfSourced)
                act("passTurn"("$otherSymbol-agent"))
            }
        }
        + achieve("play") onlyIf {
            cell(X, Y, "e").fromPercept and "turn"("self").selfSourced
        } then {
            act("put"(X, Y, mySymbol))
            act("printGrid")
            update("turn"("other").selfSourced)
            act("passTurn"("$otherSymbol-agent"))
        }

        + "endTurn"("source"("$otherSymbol-agent")) onlyIf {
            "turn"("other").selfSourced
        } then {
            - "endTurn"("source"("$otherSymbol-agent"))
            update("turn"("self").selfSourced)
            achieve("play")
        }
    }
}

fun main() {
    val system = ticTacToe(3)
//    for (agent in system.agents) {
//        println("% ${agent.name}")
//        for (plan in agent.context.planLibrary.plans) {
//            val formatter = TermFormatter.prettyExpressions(operatorSet = OperatorSet.DEFAULT + Jakta.operators)
//            println("+!${formatter.format(plan.trigger.value)} " +
//                    ": ${formatter.format(plan.guard)} " +
//                    "<- ${plan.goals.joinToString("; "){ formatter.format(it.value) }}")
//        }
//    }

//    val bb = system.agents.first().context.beliefBase.addAll(system.environment.percept()).updatedBeliefBase
//    println(bb.joinToString("\n"))
//    println(bb.solve(lp<Struct> {
//        "aligned"(listOf(cell(`_`,`_`,"x").fromPercept, cell(`_`,`_`,"x").fromPercept, cell(X, Y, "e").fromPercept))
//    }))
    system.start()
}
