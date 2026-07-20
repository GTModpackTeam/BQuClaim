package com.github.gtexpert.blpc.client.input;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import com.github.gtexpert.blpc.client.gui.Screens;

/** Registers BLPC keybinds and routes key presses to {@link Screens}. */
@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    private static final String CATEGORY = "key.categories.blpc";

    private static KeyBinding keyOpenMap;
    private static KeyBinding keyOpenParty;

    public static void init() {
        keyOpenMap = new KeyBinding("key.blpc.open_map", KeyConflictContext.IN_GAME, Keyboard.KEY_M, CATEGORY);
        keyOpenParty = new KeyBinding("key.blpc.open_party", KeyConflictContext.IN_GAME, Keyboard.KEY_P, CATEGORY);
        ClientRegistry.registerKeyBinding(keyOpenMap);
        ClientRegistry.registerKeyBinding(keyOpenParty);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyOpenMap.isPressed()) {
            Screens.openMap();
        }
        if (keyOpenParty.isPressed()) {
            Screens.openPartyDirect();
        }
    }
}
