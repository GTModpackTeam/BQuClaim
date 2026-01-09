package com.sysnote8.bquclaim.hud;

import com.sysnote8.bquclaim.ModConfig;
import com.sysnote8.bquclaim.chunk.ClaimedChunkData;
import com.sysnote8.bquclaim.chunk.ClientCache;
import com.sysnote8.bquclaim.gui.AsyncMapRenderer;
import com.sysnote8.bquclaim.gui.TextureCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MinimapHUD {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final int mapSize = 128; // ミニマップのサイズ(px)
    private final int zoomSize = 8;  // 1チャンクを何ピクセルで描くか

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // 文字などの描画が終わった後のタイミング(ALL)で描画
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        if (!ModConfig.showMinimap) return;

        // インベントリや他のGUIが開いているときは非表示
        if (mc.currentScreen != null) return;

        int pX = mc.player.chunkCoordX;
        int pZ = mc.player.chunkCoordZ;

        GlStateManager.pushMatrix();
        // 画面の右上（10pxの余白）に移動
        int startX = event.getResolution().getScaledWidth() - mapSize - 10;
        int startY = 10;
        GlStateManager.translate(startX, startY, 0);

        // 背景の黒枠
        Gui.drawRect(-1, -1, mapSize + 1, mapSize + 1, 0xFF000000);

        // 描画範囲の計算（中央がプレイヤー）
        int range = mapSize / zoomSize / 2;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                int rx = pX + x;
                int rz = pZ + z;
                int dx = (mapSize / 2) + (x * zoomSize);
                int dy = (mapSize / 2) + (z * zoomSize);

                // --- 既存のロジックを流用 ---
                renderMinimapChunk(rx, rz, dx, dy);
            }
        }

        // プレイヤーアイコン（自分）
        drawSmallPlayerIcon(mapSize / 2, mapSize / 2);

        GlStateManager.popMatrix();
    }

    private void renderMinimapChunk(int rx, int rz, int dx, int dy) {
        // 1. 地形テクスチャの取得
        int[] colors = AsyncMapRenderer.getColors(rx, rz);
        if (colors != null) {
            TextureCache.ChunkTexture tex = TextureCache.getOrCreate(rx, rz, colors);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            tex.bind();
            // zoomSizeに合わせて描画 (例: 8x8ピクセル)
            Gui.drawModalRectWithCustomSizedTexture(dx, dy, 0, 0, zoomSize, zoomSize, 16, 16);
        }

        // 2. 領地オーバーレイ (ClientCacheから取得)
        ClaimedChunkData d = ClientCache.get(rx, rz);
        if (d != null) {
            int color = d.ownerUUID.equals(mc.player.getUniqueID()) ? 0x4400FF00 : 0x44FF0000;
            Gui.drawRect(dx, dy, dx + zoomSize, dy + zoomSize, color);

            // 境界線（ミニマップが小さい場合は1pxの線で十分）
            renderSmallBorder(rx, rz, dx, dy, d.ownerUUID);
        }
    }

    private void drawSmallPlayerIcon(int centerX, int centerY) {
        float yaw = mc.player.rotationYaw;

        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("textures/map/map_icons.png"));

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.rotate(yaw, 0, 0, 1);
        // アイコンサイズを少し小さめ(6x6)にする
        GlStateManager.translate(-3, -3, 0);

        // プレイヤーの矢印テクスチャ (UV: 0, 0, Size: 8x8 on a 32x32 sheet)
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 6, 6, 32, 32);
        GlStateManager.popMatrix();
    }

    private void renderSmallBorder(int rx, int rz, int dx, int dy, java.util.UUID owner) {
        int borderColor = 0xFFFFFFFF;
        // 上下左右を確認して1pxの線を引く
        if (!isSameOwner(rx, rz - 1, owner)) Gui.drawRect(dx, dy, dx + zoomSize, dy + 1, borderColor);
        if (!isSameOwner(rx, rz + 1, owner)) Gui.drawRect(dx, dy + zoomSize - 1, dx + zoomSize, dy + zoomSize, borderColor);
        if (!isSameOwner(rx - 1, rz, owner)) Gui.drawRect(dx, dy, dx + 1, dy + zoomSize, borderColor);
        if (!isSameOwner(rx + 1, rz, owner)) Gui.drawRect(dx + zoomSize - 1, dy, dx + zoomSize, dy + zoomSize, borderColor);
    }

    private boolean isSameOwner(int rx, int rz, java.util.UUID currentOwner) {
        // クライアント側のキャッシュから隣接チャンクのデータを取得
        ClaimedChunkData neighbor = ClientCache.get(rx, rz);

        // 隣が誰のものでもない場合は false
        if (neighbor == null) return false;

        // 隣の所有者 UUID が、今描画しているチャンクの所有者と一致するかチェック
        return neighbor.ownerUUID.equals(currentOwner);
    }
}