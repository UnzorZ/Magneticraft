package com.cout970.magneticraft.item

import net.minecraft.item.ItemStack

/**
 * Created by cout970 on 2016/09/06.
 */
object ItemNugget  : ItemBase("nugget") {

    override val variants = mapOf(
            0 to "ore=iron",
            2 to "ore=copper",
            3 to "ore=lead",
            4 to "ore=cobalt",
            5 to "ore=tungsten"
    )

    override fun getUnlocalizedName(stack: ItemStack?): String =
            "${unlocalizedName}_${variants[stack?.metadata]?.removePrefix("ore=")}"
}