package com.diary.construction.domain.usecase

import kotlin.math.ceil
import kotlin.math.max

/**
 * Rough material estimates used on Pakistani / Indian sites. All sizes in feet unless noted.
 * These are thumb rules for planning, not an engineer's design.
 */
object Estimator {
    /** Local brick 8.75" x 4.25" x 2.75" with a 3/8" mortar joint. */
    private const val BRICK_CFT = 8.75 * 4.25 * 2.75 / 1728.0
    private const val BRICK_WITH_JOINT_CFT = 9.125 * 4.625 * 3.125 / 1728.0

    /** One 50 kg cement bag is about 1.25 cft. */
    const val CEMENT_BAG_CFT = 1.25

    /** Wet mortar / concrete to dry volume. */
    private const val DRY_MORTAR = 1.33
    private const val DRY_CONCRETE = 1.54

    data class WallInput(
        val lengthFt: Double,
        val heightFt: Double,
        val thicknessIn: Double = 9.0,
        val openingsSqFt: Double = 0.0,
        /** Sand parts for 1 part cement, e.g. 6 for 1:6. */
        val sandRatio: Double = 6.0,
        val wastagePercent: Double = 5.0,
    )

    data class WallResult(
        val volumeCft: Double,
        val bricks: Long,
        val cementBags: Double,
        val sandCft: Double,
    )

    fun wall(input: WallInput): WallResult {
        val area = max(0.0, input.lengthFt * input.heightFt - input.openingsSqFt)
        val volume = area * input.thicknessIn / 12.0
        val waste = 1 + input.wastagePercent / 100.0
        val bricks = volume / BRICK_WITH_JOINT_CFT
        val wetMortar = max(0.0, volume - bricks * BRICK_CFT)
        val dry = wetMortar * DRY_MORTAR
        val parts = 1 + input.sandRatio
        return WallResult(
            volumeCft = volume,
            bricks = ceil(bricks * waste).toLong(),
            cementBags = dry / parts / CEMENT_BAG_CFT * waste,
            sandCft = dry * input.sandRatio / parts * waste,
        )
    }

    data class SlabInput(
        val lengthFt: Double,
        val widthFt: Double,
        val thicknessIn: Double = 5.0,
        /** Mix 1 : sand : crush, e.g. 2 and 4 for 1:2:4. */
        val sandRatio: Double = 2.0,
        val crushRatio: Double = 4.0,
        /** Steel kg per cft of concrete (about 2.2 for a normal roof slab, ~1%). */
        val steelKgPerCft: Double = 2.2,
        val wastagePercent: Double = 5.0,
    )

    data class SlabResult(
        val volumeCft: Double,
        val cementBags: Double,
        val sandCft: Double,
        val crushCft: Double,
        val steelKg: Double,
    )

    fun slab(input: SlabInput): SlabResult {
        val volume = input.lengthFt * input.widthFt * input.thicknessIn / 12.0
        val waste = 1 + input.wastagePercent / 100.0
        val dry = volume * DRY_CONCRETE
        val parts = 1 + input.sandRatio + input.crushRatio
        return SlabResult(
            volumeCft = volume,
            cementBags = dry / parts / CEMENT_BAG_CFT * waste,
            sandCft = dry * input.sandRatio / parts * waste,
            crushCft = dry * input.crushRatio / parts * waste,
            steelKg = volume * input.steelKgPerCft * waste,
        )
    }
}
