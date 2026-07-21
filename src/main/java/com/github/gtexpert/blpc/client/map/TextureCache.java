package com.github.gtexpert.blpc.client.map;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class TextureCache {

    private static final Map<ChunkKey, ChunkTexture> CACHE = new HashMap<>();
    private static final Map<ChunkKey, Integer> HASH_CACHE = new HashMap<>();

    /**
     * Composite cache key: chunk x/z plus dimension id, so overlapping chunk coordinates in
     * different dimensions don't share a cached texture.
     */
    private static final class ChunkKey {

        final int cx, cz, dim;

        ChunkKey(int cx, int cz, int dim) {
            this.cx = cx;
            this.cz = cz;
            this.dim = dim;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ChunkKey other && other.cx == cx && other.cz == cz && other.dim == dim;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cx, cz, dim);
        }
    }

    public static ChunkTexture getOrCreate(int cx, int cz, int dim, int[] colors) {
        ChunkKey key = new ChunkKey(cx, cz, dim);
        int newHash = Arrays.hashCode(colors);

        if (HASH_CACHE.getOrDefault(key, -1) == newHash && CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        // Release old texture before replacing
        ChunkTexture old = CACHE.get(key);
        if (old != null) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(old.resourceLocation);
        }

        HASH_CACHE.put(key, newHash);
        ChunkTexture tex = new ChunkTexture(colors);
        CACHE.put(key, tex);
        return tex;
    }

    public static void clear() {
        for (ChunkTexture tex : CACHE.values()) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(tex.resourceLocation);
        }
        CACHE.clear();
        HASH_CACHE.clear();
    }

    public static class ChunkTexture {

        public final DynamicTexture texture;
        public final ResourceLocation resourceLocation;

        public ChunkTexture(int[] colors) {
            this.texture = new DynamicTexture(16, 16);
            System.arraycopy(colors, 0, texture.getTextureData(), 0, 256);
            this.texture.updateDynamicTexture();
            this.resourceLocation = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("chunk_map_" + System.nanoTime(), texture);
        }
    }
}
