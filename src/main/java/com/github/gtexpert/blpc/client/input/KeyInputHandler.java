package com.github.gtexpert.blpc.client.input;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import com.github.gtexpert.blpc.client.gui.Screens;
import com.github.gtexpert.blpc.common.ModConfig;

/** Registers BLPC keybinds and routes key presses to {@link Screens}. */
@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    private static KeyBinding keyOpenMap;
    private static KeyBinding toggleMinimap;

    private static boolean minimapVisible = ModConfig.Defaults.showMinimap;

    public static void init() {
        keyOpenMap = new KeyBinding("key.blpc.open_map", Keyboard.KEY_M, "key.categories.blpc");
        ClientRegistry.registerKeyBinding(keyOpenMap);
        toggleMinimap = new KeyBinding("key.blpc.toggle", Keyboard.KEY_N, "key.categories.blpc");
        ClientRegistry.registerKeyBinding(toggleMinimap);
    }

    public static boolean isMinimapVisible() {
        return minimapVisible;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyOpenMap.isPressed()) {
            Screens.openMap();
        }
        if (toggleMinimap.isPressed()) {
            minimapVisible = !minimapVisible;
        }
    }
}
