package com.github.gtexpert.teamclaim.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.api.modules.TModule;
import com.github.gtexpert.teamclaim.module.BaseModule;
import com.github.gtexpert.teamclaim.module.Modules;

@TModule(
         moduleID = Modules.MODULE_INTEGRATION,
         containerID = Tags.MODID,
         name = "TeamClaimMod Mod Integration",
         description = "General TeamClaimMod Integration Module. Disabling this disables all integration modules.")
public class IntegrationModule extends BaseModule {

    public static final Logger logger = LogManager.getLogger("TeamClaimMod Mod Integration");

    @NotNull
    @Override
    public Logger getLogger() {
        return logger;
    }
}
