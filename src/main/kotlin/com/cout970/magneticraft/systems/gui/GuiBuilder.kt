package com.cout970.magneticraft.systems.gui

import com.cout970.magneticraft.IVector2
import com.cout970.magneticraft.api.energy.IElectricNode
import com.cout970.magneticraft.api.heat.IHeatNode
import com.cout970.magneticraft.misc.crafting.TimedCraftingProcess
import com.cout970.magneticraft.misc.fluid.Tank
import com.cout970.magneticraft.misc.gui.*
import com.cout970.magneticraft.misc.guiTexture
import com.cout970.magneticraft.misc.inventory.InventoryRegion
import com.cout970.magneticraft.misc.network.IBD
import com.cout970.magneticraft.misc.vector.Vec2d
import com.cout970.magneticraft.misc.vector.vec2Of
import com.cout970.magneticraft.systems.gui.components.CompDynamicBackground
import com.cout970.magneticraft.systems.gui.components.CompImage
import com.cout970.magneticraft.systems.gui.components.bars.*
import com.cout970.magneticraft.systems.gui.components.buttons.ClickButton
import com.cout970.magneticraft.systems.gui.render.DrawableBox
import com.cout970.magneticraft.systems.gui.render.IComponent
import com.cout970.magneticraft.systems.tilemodules.ModuleBigCombustionChamber
import com.cout970.magneticraft.systems.tilemodules.ModuleCombustionChamber
import com.cout970.magneticraft.systems.tilemodules.ModuleInternalStorage
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.items.IItemHandler
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.max

enum class TankIO { IN, OUT, INOUT, NONE }

class GuiBuilder(val config: GuiConfig) {

    var containerConfig: (AutoContainer) -> Unit = {}
    var bars: DslBars? = null

    fun container(func: ContainerBuilder.() -> Unit) {
        containerConfig = { container ->
            ContainerBuilder(container, config).func()
        }
    }

    fun bars(func: DslBars.() -> Unit) {
        val dsl = DslBars(config)
        dsl.func()
        bars = dsl
    }

    fun build(gui: AutoGui, container: AutoContainer) {
        val bars = bars ?: return
        val sizeX = bars.getExternalSize().xi
        val startX = (gui.sizeX - sizeX) / 2

        bars.build(gui, container, vec2Of(startX, 0))
    }
}

class ContainerBuilder(val container: AutoContainer, val config: GuiConfig) {

    fun playerInventory(point: String? = null) {
        val pos = config.points[point] ?: Vec2d.ZERO
        container.bindPlayerInventory(container.player.inventory, pos)
    }

    fun slot(inventory: IItemHandler, index: Int, point: String, type: SlotType = SlotType.NORMAL, blockInput: Boolean = false) {
        val pos = config.points[point] ?: Vec2d.ZERO
        val slot = if (blockInput) {
            SlotTakeOnly(inventory, index, pos.xi, pos.yi)
        } else {
            TypedSlot(inventory, index, pos.xi, pos.yi, type)
        }
        container.addSlotToContainer(slot)
    }

    fun slotGroup(rows: Int, columns: Int, inventory: IItemHandler, startIndex: Int, point: String, type: SlotType = SlotType.NORMAL) {
        val pos = config.points[point] ?: Vec2d.ZERO

        repeat(rows) { row ->
            repeat(columns) { column ->
                val index = startIndex + column + row * columns

                val slot = if (type == SlotType.FILTER) {
                    SlotFilter(inventory, index, pos.xi + column * 18, pos.yi + row * 18)
                } else {
                    TypedSlot(inventory, index, pos.xi + column * 18, pos.yi + row * 18, type)
                }

                container.addSlotToContainer(slot)
            }
        }
    }

    fun slotButton(inventory: IItemHandler, index: Int, point: String, func: (EntityPlayer, Int) -> Unit) {
        val pos = config.points[point] ?: Vec2d.ZERO
        val slot = SlotButton(inventory, index, pos.xi, pos.yi, func)

        container.addSlotToContainer(slot)
    }

    fun region(
        first: Int,
        size: Int,
        inverseDirection: Boolean = false,
        filter: (ItemStack, Int) -> Boolean = { _, _ -> true }
    ) {
        container.inventoryRegions += InventoryRegion(first until first + size, inverseDirection, filter)
    }

    fun onClick(id: String, func: () -> Unit) {
        container.buttonListeners[id] = func
    }

    fun sendToServer(data: IBD) {
        container.sendUpdate(data)
    }

    fun receiveDataFromClient(func: (IBD) -> Unit) {
        container.receiveDataFromClientFunc = func
    }
}

private val ICON_SIZE = vec2Of(0, 10)
private const val ICON_HEIGHT = 10

class DslBars(val config: GuiConfig) {
    private val components = mutableListOf<Pair<IVector2, (IVector2) -> List<IComponent>>>()
    var marginX = 4
    var marginY = 4

    var paddingX = 1

    private fun barOf(texture: IVector2,
                      value: () -> Double, tooltip: () -> List<String> = { emptyList() },
                      icon: Int): Pair<IVector2, (IVector2) -> List<IComponent>> {

        return Vec2d(12, 54 + ICON_HEIGHT) to { pos ->
            listOf(
                GuiValueBar(
                    pos = pos + ICON_SIZE,
                    size = vec2Of(12, 54),
                    colorTextureOffset = texture,
                    value = value,
                    tooltip = tooltip
                ),
                iconOf(icon, pos + vec2Of(2, 0))
            )
        }
    }

    private fun bar2Of(texture: IVector2, value: () -> Double, tooltip: () -> List<String> = { emptyList() },
                       icon: Int): Pair<IVector2, (IVector2) -> List<IComponent>> {

        return Vec2d(12, 54 + ICON_HEIGHT) to { pos ->
            listOf(
                GuiValueBar(
                    pos = pos + ICON_SIZE + vec2Of(1, 0),
                    size = vec2Of(10, 54),
                    backgroundTextureOffset = vec2Of(10, 175),
                    colorTextureOffset = texture,
                    value = value,
                    tooltip = tooltip
                ),
                iconOf(icon, pos + vec2Of(2, 0))
            )
        }
    }

    fun electricBar(node: IElectricNode) {
        components += barOf(
            texture = vec2Of(23, 121),
            value = { node.voltage / 120.0 },
            tooltip = { listOf(String.format("%.2fV", node.voltage)) },
            icon = 1
        )
    }

    fun storageBar(node: ModuleInternalStorage) {
        val numberFormat = DecimalFormat("#,###", DecimalFormatSymbols().apply { groupingSeparator = ' ' })
        components += barOf(
            texture = vec2Of(45, 121),
            value = { node.energy / node.capacity.toDouble() },
            tooltip = { listOf(numberFormat.format(node.energy) + "J") },
            icon = 0
        )
    }

    fun heatBar(node: IHeatNode) {
        components += barOf(
            texture = vec2Of(34, 121),
            value = { node.temperature / 4000.0 },
            tooltip = { listOf(formatHeat(node.temperature)) },
            icon = 2
        )
    }

    fun rfBar(node: IEnergyStorage) {
        val numberFormat = DecimalFormat("#,###", DecimalFormatSymbols().apply { groupingSeparator = ' ' })
        components += barOf(
            texture = vec2Of(57, 121),
            value = { node.energyStored / node.maxEnergyStored.toDouble() },
            tooltip = { listOf(numberFormat.format(node.energyStored) + "RF") },
            icon = 8
        )
    }

    fun genericBar(index: Int, icon: Int, prov: IBarProvider, tooltip: () -> List<String>) {
        components += Vec2d(7, 60) to { pos -> listOf(CompDynamicBar(prov, index, pos + ICON_SIZE, tooltip), iconOf(icon, pos)) }
    }

    fun consumptionBar(va: ValueAverage, limit: Number) {
        components += bar2Of(
            texture = vec2Of(39, 176),
            value = { va.storage / limit.toDouble() },
            tooltip = { listOf(String.format("Consumption: %.2fW", va.storage)) },
            icon = 5
        )
    }

    fun slotSpacer(rows: Int = 1, columns: Int = 1) {
        components.add(Pair(vec2Of(18 * columns, 18 * rows), { pos ->
            @Suppress("RemoveExplicitTypeArguments")
            emptyList<IComponent>()
        }))
    }

    fun fuelBar(mod: ModuleCombustionChamber) {
        val value = {
            if (mod.maxBurningTime == 0) 0.0 else {
                (mod.maxBurningTime - mod.burningTime) / mod.maxBurningTime.toDouble()
            }
        }
        components += bar2Of(
            texture = vec2Of(21, 176),
            value = value,
            tooltip = { listOf(String.format("Fuel: %.1f%%", 100.0 * value())) },
            icon = 5
        )
    }

    fun fuelBar(mod: ModuleBigCombustionChamber) {
        val value = {
            if (mod.maxBurningTime == 0) 0.0 else {
                (mod.maxBurningTime - mod.burningTime) / mod.maxBurningTime.toDouble()
            }
        }
        components += bar2Of(
            texture = vec2Of(21, 176),
            value = value,
            tooltip = { listOf(String.format("Fuel: %.1f%%", 100.0 * value())) },
            icon = 5
        )
    }

    fun productionBar(va: ValueAverage, limit: Number) {
        components += bar2Of(
            texture = vec2Of(30, 176),
            value = { va.storage / limit.toDouble() },
            tooltip = { listOf(String.format("Production: %.2fW", va.storage)) },
            icon = 4
        )
    }

    fun machineFluidBar(va: ValueAverage, limit: Number) {
        components += bar2Of(
            texture = vec2Of(30, 176),
            value = { va.storage / limit.toDouble() },
            tooltip = { listOf(String.format("%.1f mB/t", va.storage)) },
            icon = 4
        )
    }

    fun electricTransferBar(va: ValueAverage, min: Number, max: Number) {
        components += Vec2d(7, 54 + ICON_HEIGHT) to { pos ->
            listOf(
                TransferRateBar(
                    pos = pos + ICON_SIZE + vec2Of(1, 3),
                    value = { va.storage.toDouble() },
                    base = { 0.0 },
                    min = { min.toDouble() },
                    max = { max.toDouble() }
                ),
                iconOf(10, pos)
            )
        }
    }

    fun progressBar(timed: TimedCraftingProcess) {
        val value = { timed.timer / timed.limit().toDouble() }
        components += bar2Of(
            texture = vec2Of(66, 176),
            value = value,
            tooltip = { listOf(String.format("Progress: %.1f%%", 100 * value())) },
            icon = 4
        )
    }

    fun tank(tank: Tank, use: TankIO) {
        components += Vec2d(18, 50) to { pos ->
            val icons = when (use) {
                TankIO.IN -> listOf(iconOf(5, pos + vec2Of(6, 0)))
                TankIO.OUT -> listOf(iconOf(3, pos + vec2Of(6, 0)))
                TankIO.INOUT -> listOf(iconOf(5, pos + vec2Of(2, 0)), iconOf(3, pos + vec2Of(9, 0)))
                TankIO.NONE -> emptyList()
            }
            listOf(CompFluidBar2(pos + ICON_SIZE, tank)) + icons
        }
    }

    fun drawable(hitbox: IVector2, offset: String, size: String, uv: String) {
        val offsetVec = config.points[offset] ?: Vec2d.ZERO
        val sizeVec = config.points[size] ?: Vec2d.ZERO
        val texVec = config.points[uv] ?: Vec2d.ZERO
        components.add(Pair(hitbox, { pos ->
            listOf(CompDrawable(pos + offsetVec, sizeVec, texVec))
        }))
    }

    fun light(offset: String, size: String, uv: String, callback: () -> Boolean) {
        val offsetVec = config.points[offset] ?: Vec2d.ZERO
        val sizeVec = config.points[size] ?: Vec2d.ZERO
        val texVec = config.points[uv] ?: Vec2d.ZERO

        components.add(Pair(sizeVec, { pos ->
            listOf(CompDrawable(pos + offsetVec, sizeVec, texVec, callback))
        }))
    }

    fun clickButton(id: String, offset: String) {
        val offsetVec = config.points[offset] ?: Vec2d.ZERO
        components.add(Pair(Vec2d(18, 18), { pos ->
            listOf(ClickButton(pos + offsetVec, id))
        }))
    }

    private fun iconOf(index: Int, pos: IVector2): CompImage = CompImage(guiTexture("misc"), DrawableBox(
        screenPos = pos,
        screenSize = vec2Of(7, 8),
        texturePos = vec2Of(37 + (index % 8) * 8, 62 + (index / 8) * 9),
        textureSize = vec2Of(7, 8),
        textureScale = vec2Of(256)
    ))

    fun getInternalSize(): IVector2 {
        val sizeX = components.sumByDouble { it.first.x + paddingX * 2 }
        val sizeY = components.fold(0.0) { acc, it -> max(acc, it.first.y) }
        return vec2Of(sizeX, sizeY)
    }

    fun getExternalSize(): IVector2 {
        val size = getInternalSize()
        return vec2Of(size.x + marginX * 2, size.y + marginY * 2)
    }

    fun build(gui: AutoGui, container: AutoContainer, pos: IVector2) {
        var startX = pos.xi + marginX
        val startY = pos.yi + marginY

        gui.components += CompDynamicBackground(pos, getExternalSize())

        components.forEach { (compSize, factory) ->
            val compPos = vec2Of(startX + paddingX, startY)
            startX += compSize.xi + paddingX * 2
            gui.components += factory(compPos)
        }

        gui.components.filterIsInstance<ClickButton>().forEach {
            val listener = container.buttonListeners[it.id]
            if (listener != null) {
                it.onClick = listener
            }
        }
    }
}