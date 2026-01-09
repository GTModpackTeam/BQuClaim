package com.sysnote8.bquclaim.gui;

import com.sysnote8.bquclaim.BQPartyHelper;
import com.sysnote8.bquclaim.chunk.ClaimedChunkData;
import com.sysnote8.bquclaim.chunk.ClientCache;
import com.sysnote8.bquclaim.network.MessageClaimChunk;
import com.sysnote8.bquclaim.network.ModNetwork;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class GuiChunkMap extends GuiScreen {

    private final int size = 16; // 1チャンクの表示サイズ
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int cx = width / 2, cy = height / 2;
        int pX = mc.player.chunkCoordX, pZ = mc.player.chunkCoordZ;

        // 半径10チャンク分を描画
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                int rx = pX + x;
                int rz = pZ + z;
                int dx = cx + (x * size);
                int dy = cy + (z * size);

                int[] colors = AsyncMapRenderer.getColors(rx, rz);

                if (colors != null) {
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    // データがあればテクスチャをバインドして描画
                    TextureCache.ChunkTexture tex = TextureCache.getOrCreate(rx, rz, colors);
                    tex.bind();
                    Gui.drawModalRectWithCustomSizedTexture(dx, dy, 0, 0, size, size, 16, 16);
                } else {
                    // データがなければ非同期計算をリクエスト
                    AsyncMapRenderer.requestChunk(mc.world, rx, rz);
                    // ロード中として暗い四角を描画
                    drawRect(dx, dy, dx + size, dy + size, 0xFF222222);
                }

                // その上に領地を重ねる
                renderClaimOverlay(rx, rz, dx, dy);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderClaimOverlay(int rx, int rz, int dx, int dy) {
        // クライアント側のキャッシュから領地データを取得
        ClaimedChunkData d = ClientCache.get(rx, rz);

        if (d != null) {
            int color;
            // 自分の領地
            if (d.ownerUUID.equals(mc.player.getUniqueID())) {
                color = 0x5500FF00; // 半透明の緑
            }
            // BQのパーティー仲間
            else if (BQPartyHelper.areInSameParty(mc.player.getUniqueID(), d.ownerUUID)) {
                color = 0x5500FFFF; // 半透明の水色
            }
            // 他人の領地
            else {
                color = 0x55FF0000; // 半透明の赤
            }

            // 地形が見えるように、drawRectで半透明の四角を重ねる
            // dx, dy は画面上の描画開始位置、size はチャンクの表示サイズ(16)
            drawRect(dx, dy, dx + size, dy + size, color);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        // ドラッグ中のチャンク座標計算
        int rx = (mx - (width / 2)) / size + mc.player.chunkCoordX;
        int rz = (my - (height / 2)) / size + mc.player.chunkCoordZ;

        if (rx != lastX || rz != lastZ) {
            // 左ドラッグ(0)でClaim, 右ドラッグ(1)でUnclaim
            ModNetwork.INSTANCE.sendToServer(new MessageClaimChunk(rx, rz, btn == 0 ? 0 : 1));
            lastX = rx;
            lastZ = rz;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        lastX = Integer.MIN_VALUE;
        lastZ = Integer.MIN_VALUE;
    }
}
