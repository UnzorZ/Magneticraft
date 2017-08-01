package com.cout970.magneticraft.gui.client

import com.cout970.magneticraft.IVector2
import com.cout970.magneticraft.computer.DeviceMonitor
import com.cout970.magneticraft.computer.Motherboard
import com.cout970.magneticraft.config.Config
import com.cout970.magneticraft.gui.client.components.*
import com.cout970.magneticraft.gui.client.components.bars.BatteryStorageBar
import com.cout970.magneticraft.gui.client.components.bars.CallbackBarProvider
import com.cout970.magneticraft.gui.client.components.bars.CompVerticalBar
import com.cout970.magneticraft.gui.client.components.bars.TransferRateBar
import com.cout970.magneticraft.gui.client.components.buttons.AbstractButton
import com.cout970.magneticraft.gui.client.components.buttons.ButtonState
import com.cout970.magneticraft.gui.client.components.buttons.MultiButton
import com.cout970.magneticraft.gui.client.components.buttons.SimpleButton
import com.cout970.magneticraft.gui.client.core.DrawableBox
import com.cout970.magneticraft.gui.client.core.GuiBase
import com.cout970.magneticraft.gui.common.ContainerComputer
import com.cout970.magneticraft.gui.common.ContainerShelvingUnit
import com.cout970.magneticraft.gui.common.core.ContainerBase
import com.cout970.magneticraft.misc.gui.formatHeat
import com.cout970.magneticraft.misc.network.IBD
import com.cout970.magneticraft.tileentity.TileBattery
import com.cout970.magneticraft.tileentity.TileCombustionChamber
import com.cout970.magneticraft.tileentity.TileElectricFurnace
import com.cout970.magneticraft.tileentity.modules.ModuleShelvingUnit
import com.cout970.magneticraft.util.guiTexture
import com.cout970.magneticraft.util.toKelvinFromCelsius
import com.cout970.magneticraft.util.vector.Vec2d
import com.cout970.magneticraft.util.vector.vec2Of
import org.lwjgl.input.Keyboard

/**
 * Created by cout970 on 2017/06/12.
 */

class GuiBox(container: ContainerBase) : GuiBase(container) {

    override fun initComponents() {
        components.add(CompBackground(guiTexture("box")))
    }
}

class GuiElectricFurnace(container: ContainerBase) : GuiBase(container) {

    val tile = (container.tileEntity as TileElectricFurnace)

    override fun initComponents() {
        components.add(CompBackground(guiTexture("electric_furnace")))
        components.add(CompElectricBar(tile.node, Vec2d(58, 16)))
        val consumptionCallback = CallbackBarProvider(
                callback = { tile.processModule.production.storage.toDouble() },
                max = { Config.electricFurnaceMaxConsumption },
                min = { 0.0 }
        )
        components.add(CompVerticalBar(consumptionCallback, 3,
                Vec2d(69, 16),
                { listOf(String.format("%.2fW", consumptionCallback.callback())) }))

        val processCallback = CallbackBarProvider(
                { tile.processModule.timedProcess.timer.toDouble() },
                { tile.processModule.timedProcess.limit().toDouble() },
                { 0.0 }
        )
        components.add(
                CompVerticalBar(processCallback, 2, Vec2d(80, 16),
                        { listOf("Burning: " + "%.1f".format(processCallback.getLevel() * 100) + "%") }))
    }
}

class GuiBattery(container: ContainerBase) : GuiBase(container) {

    val tile = container.tileEntity as TileBattery

    override fun initComponents() {
        components.add(CompBackground(guiTexture("battery")))
        components.add(CompElectricBar(tile.node, Vec2d(47, 16)))

        components.add(BatteryStorageBar(this, guiTexture("battery"),
                tile.storageModule))

        components.add(TransferRateBar(
                value = { tile.storageModule.chargeRate.storage.toDouble() },
                min = { -tile.storageModule.maxChargeSpeed },
                base = { 0.0 },
                max = { tile.storageModule.maxChargeSpeed }, pos = Vec2d(58, 16))
        )

        components.add(TransferRateBar(
                { tile.itemChargeModule.itemChargeRate.storage.toDouble() },
                { -tile.itemChargeModule.transferRate.toDouble() },
                { 0.0 },
                { tile.itemChargeModule.transferRate.toDouble() }, Vec2d(58 + 33, 16))
        )
    }
}

class GuiShelvingUnit(container: ContainerBase) : GuiBase(container) {

    override fun initComponents() {
        xSize = 194
        ySize = 207
        val texture = guiTexture("shelving_unit")
        components.add(CompBackground(texture, size = vec2Of(194, 207)))
        val scrollBar = CompScrollBar(vec2Of(174, 21), texture = texture)
        val textInput = CompTextInput(fontRenderer, vec2Of(10, 7), vec2Of(86, 13))
        val button1Map = mapOf(
                ButtonState.UNPRESSED to (vec2Of(194, 75) to vec2Of(23, 24)),
                ButtonState.HOVER_UNPRESSED to (vec2Of(194, 75) to vec2Of(23, 24)),
                ButtonState.PRESSED to (vec2Of(194, 0) to vec2Of(23, 24)),
                ButtonState.HOVER_PRESSED to (vec2Of(194, 0) to vec2Of(23, 24))
        )
        val button2Map = mapOf(
                ButtonState.UNPRESSED to (vec2Of(194, 100) to vec2Of(23, 24)),
                ButtonState.HOVER_UNPRESSED to (vec2Of(194, 100) to vec2Of(23, 24)),
                ButtonState.PRESSED to (vec2Of(194, 25) to vec2Of(23, 24)),
                ButtonState.HOVER_PRESSED to (vec2Of(194, 25) to vec2Of(23, 24))
        )
        val button3Map = mapOf(
                ButtonState.UNPRESSED to (vec2Of(194, 125) to vec2Of(23, 24)),
                ButtonState.HOVER_UNPRESSED to (vec2Of(194, 125) to vec2Of(23, 24)),
                ButtonState.PRESSED to (vec2Of(194, 50) to vec2Of(23, 24)),
                ButtonState.HOVER_PRESSED to (vec2Of(194, 50) to vec2Of(23, 24))
        )
        val buttons = listOf(
                MultiButton(2, texture, vec2Of(176, 129) to vec2Of(23, 24), vec2Of(256), { button1Map[it]!! }),
                MultiButton(1, texture, vec2Of(176, 154) to vec2Of(23, 24), vec2Of(256), { button2Map[it]!! }),
                MultiButton(0, texture, vec2Of(176, 179) to vec2Of(23, 24), vec2Of(256), { button3Map[it]!! })
        )
        components.add(scrollBar)
        components.add(textInput)
        buttons.forEach { components.add(it); it.listener = this::onPress; it.allButtons = buttons }
        components.add(CompShelvingUnit(container as ContainerShelvingUnit, scrollBar, textInput))

        (container as? ContainerShelvingUnit)?.let {
            buttons[2 - it.level.levelIndex].state = ButtonState.PRESSED
        }
        Keyboard.enableRepeatEvents(true)
    }

    fun onPress(button: AbstractButton, mouse: Vec2d, mouseButton: Int): Boolean {
        val ibd = IBD().apply { setInteger(2, button.id) }
        container.sendUpdate(ibd)
        (container as ContainerShelvingUnit).switchLevel(ModuleShelvingUnit.Level.values()[button.id])
        return true
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }
}

class GuiComputer(container: ContainerBase) : GuiBase(container) {

    val monitor: DeviceMonitor = (container as ContainerComputer).monitor
    val motherboard: Motherboard = (container as ContainerComputer).motherboard

    init {
        xSize = 350
        ySize = 255
    }

    override fun initComponents() {
        val texture = guiTexture("old_monitor")
        val textureSize = Vec2d(512, 512)
        val buttonSize = vec2Of(8, 8)
        components.add(CompBackground(texture, textureSize = textureSize, size = Vec2d(350, 230)))
        components.add(MonitorComponent(monitor))
        components.add(SimpleButton(0, texture, vec2Of(23, 220) to buttonSize, textureSize, this::getUV))
        components.add(SimpleButton(1, texture, vec2Of(33, 220) to buttonSize, textureSize, this::getUV))
        components.add(SimpleButton(2, texture, vec2Of(43, 220) to buttonSize, textureSize, this::getUV))

        components.add(CompLight(
                on = DrawableBox(pos + Vec2d(14, 221) to vec2Of(7, 7), vec2Of(7, 238) to vec2Of(7, 7), textureSize),
                off = DrawableBox(pos + Vec2d(14, 221) to vec2Of(7, 7), vec2Of(0, 238) to vec2Of(7, 7), textureSize),
                texture = texture, condition = { motherboard.isOnline() }
        ))
        components.filterIsInstance<SimpleButton>().forEach { it.listener = this::onPress }
    }

    fun getUV(state: ButtonState): Pair<IVector2, IVector2> {
        return when (state) {
            ButtonState.UNPRESSED -> vec2Of(0, 230) to vec2Of(8)
            ButtonState.PRESSED -> vec2Of(16, 230) to vec2Of(8)
            ButtonState.HOVER_UNPRESSED -> vec2Of(8, 230) to vec2Of(8)
            ButtonState.HOVER_PRESSED -> vec2Of(16, 230) to vec2Of(8)
        }
    }

    fun onPress(button: AbstractButton, mouse: Vec2d, mouseButton: Int): Boolean {
        val ibd = IBD().apply { setInteger(50, button.id) }
        container.sendUpdate(ibd)
        return true
    }
}

class GuiCombustionChamber(container: ContainerBase) : GuiBase(container) {

    val tile = (container.tileEntity as TileCombustionChamber)

    override fun initComponents() {
        components.add(CompBackground(guiTexture("combustion_chamber")))
        val burningCallback = CallbackBarProvider(
                { tile.combustionChamberModule.maxBurningTime.toDouble() - tile.combustionChamberModule.burningTime.toDouble() },
                { tile.combustionChamberModule.maxBurningTime.toDouble() },
                { 0.0 }
        )
        components.add(
                CompVerticalBar(burningCallback, 1, Vec2d(69, 16),
                        { listOf(String.format("Fuel: %.1f%%", burningCallback.getLevel() * 100)) }))

        val heatCallback = CallbackBarProvider(
                { tile.combustionChamberModule.heat.toDouble() },
                { 99.5.toKelvinFromCelsius() },
                { 24.0.toKelvinFromCelsius() }
        )
        components.add(
                CompVerticalBar(heatCallback, 2, Vec2d(80, 16),
                        { listOf("Heat: " + formatHeat(heatCallback.callback())) }))
    }
}


