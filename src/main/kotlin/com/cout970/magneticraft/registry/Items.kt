package com.cout970.magneticraft.registry

import com.cout970.magneticraft.item.ElectricItems
import com.cout970.magneticraft.item.Metals
import com.cout970.magneticraft.item.Tools
import net.minecraft.item.Item
import net.minecraftforge.fml.common.registry.ForgeRegistries

/**
 * Created by cout970 on 2017/03/26.
 */

var items: List<Item> = emptyList()
    private set

fun initItems() {
    val items_ = mutableListOf<Item>()

    items_ += Metals.initItems()
    items_ += Tools.initItems()
    items_ += ElectricItems.initItems()

    items_.forEach { ForgeRegistries.ITEMS.register(it) }
    items = items_
}
// TODO list
//listOf<Item>(
//        ItemGuideBook,
//        ItemCrushedOre,
//        ItemCrushedCoal,
//        ItemCrushedLapis,
//        ItemWoodChip,
//        ItemPulpWood,
//        ItemPebbles,
//        ItemPebblesCoal,
//        ItemPebblesLapis,
//        ItemMesh,
//        ItemCoilOfWire,
//        ItemVoltmeter,
//        ItemThermometer,
//        ItemBattery,
//        ItemFloppyDisk,
//        ItemCoke
//)