package com.sysnote8.bquclaim.gui;

import com.sysnote8.bquclaim.ModConfig;
import com.sysnote8.bquclaim.Tags;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class KeyInputHandler {
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ModKeyBindings.keyOpenMap.isPressed()) {
            // プレイヤーがGUIを開いていない時だけ開く
            if (Minecraft.getMinecraft().currentScreen == null) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiChunkMap());
            }
        }
        if(ModKeyBindings.toggleMinimap.isPressed()) {
            ModConfig.showMinimap = !ModConfig.showMinimap;
            ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
        }
    }
}
