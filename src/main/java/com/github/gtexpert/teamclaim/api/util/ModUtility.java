package com.github.gtexpert.teamclaim.api.util;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.teamclaim.Tags;

public class ModUtility {

    public static @NotNull ResourceLocation id(String path) {
        return new ResourceLocation(Tags.MODID, path);
    }
}
