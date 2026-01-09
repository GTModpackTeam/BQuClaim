package com.sysnote8.bquclaim.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ChunkMapTexture {
    private final DynamicTexture texture;
    private final ResourceLocation resourceLocation;
    private final int[] data;

    public ChunkMapTexture() {
        this.texture = new DynamicTexture(16, 16);
        this.data = texture.getTextureData();
        this.resourceLocation = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("chunk_map", texture);
    }

    public void generate(World world, int chunkX, int chunkZ) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                data[x + z * 16] = MapColorHelper.getBlockColor(world, (chunkX << 4) + x, (chunkZ << 4) + z);
            }
        }
        texture.updateDynamicTexture(); // GPUへ転送
    }

    public void bind() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
    }
}
