package com.github.gtexpert.blpc.integration.jmap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import journeymap.client.ui.option.AddonOptionsManager;

/**
 * Opens JourneyMap's Addon Options screen directly. All BLPC-specific JourneyMap settings
 * (claim overlay toggle, waypoint sharing, sync interval) are registered through
 * {@link journeymap.api.v2.common.option.OptionsRegistry} in {@link JMapPlugin}, so they
 * appear natively in JourneyMap's own settings UI.
 */
@SideOnly(Side.CLIENT)
public final class JMapSettingsPanel {

    public static final String PANEL_ID = "blpc.party.addons.journeymap";

    private JMapSettingsPanel() {}

    public static void open() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new AddonOptionsManager(mc.currentScreen, false));
    }
}
