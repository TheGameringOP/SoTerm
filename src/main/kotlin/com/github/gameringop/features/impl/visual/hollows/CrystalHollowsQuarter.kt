package com.github.gameringop.features.impl.visual.hollows

import net.minecraft.core.BlockPos
import java.util.function.Predicate

enum class CrystalHollowsQuarter(private val predicate: Predicate<BlockPos>) {
    NUCLEUS(Predicate { pos ->
        pos.x in 449..576 && pos.z in 449..576
    }),
    JUNGLE(Predicate { pos ->
        pos.x <= 576 && pos.z <= 576
    }),
    PRECURSOR_REMNANTS(Predicate { pos ->
        pos.x > 448 && pos.z > 448
    }),
    GOBLIN_HOLDOUT(Predicate { pos ->
        pos.x <= 576 && pos.z > 448
    }),
    MITHRIL_DEPOSITS(Predicate { pos ->
        pos.x > 448 && pos.z <= 576
    }),
    MAGMA_FIELDS(Predicate { pos ->
        pos.y < 80
    }),
    OUT_OF_BOUND(Predicate { pos ->
        pos.x > 824 || pos.z > 824 || pos.x < 201 || pos.z < 201
    }),
    ANY(Predicate { true });

    fun testPredicate(blockPos: BlockPos): Boolean = predicate.test(blockPos)
}
