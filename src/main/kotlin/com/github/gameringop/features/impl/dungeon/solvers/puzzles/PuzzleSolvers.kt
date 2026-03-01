package com.github.gameringop.features.impl.dungeon.solvers.puzzles

import com.github.gameringop.event.impl.*
import com.github.gameringop.features.Feature
import com.github.gameringop.ui.clickgui.components.getValue
import com.github.gameringop.ui.clickgui.components.impl.ColorSetting
import com.github.gameringop.ui.clickgui.components.impl.SliderSetting
import com.github.gameringop.ui.clickgui.components.impl.ToggleSetting
import com.github.gameringop.ui.clickgui.components.provideDelegate
import com.github.gameringop.ui.clickgui.components.section
import com.github.gameringop.ui.clickgui.components.showIf
import com.github.gameringop.utils.ColorUtils.withAlpha
import java.awt.Color

object PuzzleSolvers: Feature() {
    val blaze by ToggleSetting("Enabled").section("Blaze Solver")
    val blazeCount by SliderSetting("Blazes to Highlight", 3, 1, 10, 1).showIf { blaze.value }
    val firstBlazeColor by ColorSetting("First Blaze", Color.GREEN).showIf { blaze.value }
    val secondBlazeColor by ColorSetting("Second Blaze", Color.YELLOW).showIf { blaze.value }
    val thirdBlazeColor by ColorSetting("Other Blazes", Color.RED).showIf { blaze.value }
    val lineColor by ColorSetting("Line Color", Color.WHITE).showIf { blaze.value }

    val boulder by ToggleSetting("Enabled ").section("Boulder Solver")
    val showAll by ToggleSetting("Show All Moves", false).showIf { boulder.value }
    val boxColor by ColorSetting("Boulder Box Color", Color.BLUE.withAlpha(100)).showIf { boulder.value }
    val clickColor by ColorSetting("Click Color", Color.RED.withAlpha(100)).showIf { boulder.value }

    val creeper by ToggleSetting("Enabled  ").section("Creeper Beam Solver")
    val renderLines by ToggleSetting("Render Lines", true).showIf { creeper.value }
    val phase by ToggleSetting("Phase Through Walls", true).showIf { creeper.value }

    val quiz by ToggleSetting("Enabled   ").section("Quiz Solver")
    val answerColor by ColorSetting("Answer Color", Color.CYAN.withAlpha(128)).showIf { quiz.value }
    val quizTimer by ToggleSetting("Quiz Timer", true).showIf { quiz.value }

    val tpmaze by ToggleSetting("Enabled    ").section("Teleport Maze Solver")
    val correctTpPadColor by ColorSetting("Correct Pad Color", Color.GREEN).showIf { tpmaze.value }
    val wrongTpPadColor by ColorSetting("Wrong Pad Color", Color.RED).showIf { tpmaze.value }

    val weirdos by ToggleSetting("Enabled     ").section("Three Weirdos Solver")
    val removeChests by ToggleSetting("Hide Wrong Chests", true).showIf { weirdos.value }
    val colorCorrect by ColorSetting("Correct Chest Color", Color.GREEN).showIf { weirdos.value }
    val colorWrong by ColorSetting("Wrong Chest Color", Color.RED).showIf { weirdos.value }

    val ttt by ToggleSetting("Enabled      ").section("TicTacToe Solver")
    val preventMissClick by ToggleSetting("Prevent Miss Click", true).showIf { ttt.value }
    val color by ColorSetting("Highlight Color", Color.GREEN).showIf { ttt.value }
    val prediction by ToggleSetting("Prediction").showIf { ttt.value }
    val predictionColor by ColorSetting("Prediction Color", Color.ORANGE).showIf { ttt.value && prediction.value }

    val water by ToggleSetting("Enabled       ").section("Water Board Solver")
    val currentClickColor by ColorSetting("Click Color", Color.GREEN).showIf { water.value }
    val nextColor by ColorSetting("Next Click Color", Color.YELLOW).showIf { water.value }

    val icefill by ToggleSetting("Enabled        ").section("Ice Fill Solver")
    val icefillColor by ColorSetting("Click Color", Color.GREEN).showIf { icefill.value }

    override fun init() {
        register<WorldChangeEvent> {
            if (blaze.value) BlazeSolver.reset()
            if (boulder.value) BoulderSolver.reset()
            if (creeper.value) CreeperBeamSolver.reset()
            if (quiz.value) QuizSolver.reset()
            if (tpmaze.value) TeleportMazeSolver.reset()
            if (weirdos.value) ThreeWeirdosSolver.reset()
            if (ttt.value) TicTacToeSolver.reset()
            if (water.value) WaterBoardSolver.reset()
            if (icefill.value) IceFillSolver.reset()
        }

        register<DungeonEvent.RoomEvent.onExit> {
            if (blaze.value) BlazeSolver.reset()
            if (boulder.value) BoulderSolver.reset()
            if (creeper.value) CreeperBeamSolver.reset()
            if (tpmaze.value) TeleportMazeSolver.reset()
            if (ttt.value) TicTacToeSolver.reset()
            if (water.value) WaterBoardSolver.reset()
            if (icefill.value) IceFillSolver.reset()
        }

        register<DungeonEvent.RoomEvent.onStateChange> {
            if (creeper.value) CreeperBeamSolver.onStateChange(event)
            if (weirdos.value) ThreeWeirdosSolver.onStateChange(event)
            if (ttt.value) TicTacToeSolver.onStateChange(event)
        }

        register<DungeonEvent.RoomEvent.onEnter> {
            if (blaze.value) BlazeSolver.onRoomEnter(event)
            if (boulder.value) BoulderSolver.onRoomEnter(event)
            if (creeper.value) CreeperBeamSolver.onRoomEnter(event)
            if (quiz.value) QuizSolver.onRoomEnter(event)
            if (tpmaze.value) TeleportMazeSolver.onRoomEnter(event)
            if (ttt.value) TicTacToeSolver.onRoomEnter(event)
            if (water.value) WaterBoardSolver.onRoomEnter(event)
            if (icefill.value) IceFillSolver.onRoomEnter(event)
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (tpmaze.value) TeleportMazeSolver.onPacket(event)
            if (ttt.value) TicTacToeSolver.onPacket(event)
        }

        register<ChatMessageEvent> {
            if (quiz.value) QuizSolver.onChat(event)
            if (weirdos.value) ThreeWeirdosSolver.onChat(event)
        }

        register<CheckEntityGlowEvent> {
            if (blaze.value) BlazeSolver.onEntityGlow(event)
        }

        register<RenderWorldEvent> {
            if (blaze.value) BlazeSolver.onRenderWorld(event.ctx)
            if (boulder.value) BoulderSolver.onRenderWorld(event.ctx)
            if (creeper.value) CreeperBeamSolver.onRenderWorld(event.ctx)
            if (quiz.value) QuizSolver.onRenderWorld(event.ctx)
            if (tpmaze.value) TeleportMazeSolver.onRenderWorld(event.ctx)
            if (weirdos.value) ThreeWeirdosSolver.onRenderWorld(event.ctx)
            if (ttt.value) TicTacToeSolver.onRenderWorld(event.ctx)
            if (water.value) WaterBoardSolver.onRenderWorld(event.ctx)
            if (icefill.value) IceFillSolver.onRenderWorld(event.ctx)
        }

        register<RenderOverlayEvent> {
            if (quiz.value) QuizSolver.onRenderOverlay(event.context)
        }

        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> {
            if (boulder.value) BoulderSolver.onInteract(event)
            if (weirdos.value) ThreeWeirdosSolver.onInteract(event)
            if (ttt.value) TicTacToeSolver.onInteract(event)
            if (water.value) WaterBoardSolver.onInteract(event)
        }
    }
}