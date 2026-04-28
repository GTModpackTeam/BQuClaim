package com.github.gtexpert.blpc.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.ClientGUI;

import com.github.gtexpert.blpc.common.ModConfig;

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

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyOpenMap.isPressed()) {
            if (Minecraft.getMinecraft().currentScreen == null) {
                ClientGUI.open(new ChunkMapScreen());
            }
        }
        if (toggleMinimap.isPressed()) {
            minimapVisible = !minimapVisible;
        }
    }
}
