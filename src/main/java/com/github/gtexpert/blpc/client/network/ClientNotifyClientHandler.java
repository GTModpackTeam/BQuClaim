package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.api.party.RelationType;
import com.github.gtexpert.blpc.client.gui.BLPCToast;
import com.github.gtexpert.blpc.common.network.message.ClientNotify;

/**
 * Client-side handler for {@link ClientNotify}. Dispatches by
 * {@link ClientNotify#getKind() kind} to the appropriate
 * {@link BLPCToast} configuration. Replaces the per-kind handlers
 * (chunk transit / party event / claim failed) — adding a new kind only
 * requires a new {@code case} arm here, no new wire ID.
 */
@SideOnly(Side.CLIENT)
public final class ClientNotifyClientHandler extends MainThreadMessageHandler<ClientNotify> {

    @Override
    protected void handleOnMainThread(ClientNotify msg) {
        BLPCToast toast = buildToast(msg);
        if (toast != null) {
            Minecraft.getMinecraft().getToastGui().add(toast);
        }
    }

    private static BLPCToast buildToast(ClientNotify msg) {
        return switch (msg.getKind()) {
            case ClientNotify.KIND_CHUNK_TRANSIT -> BLPCToast.builder()
                    .fromTransit(RelationType.fromName(msg.getRelationName()), msg.isEntered(), msg.getPlayerName(),
                            msg.getPlayerUUID(), msg.getOwnerName(), msg.isSelf())
                    .build();
            case ClientNotify.KIND_PARTY_EVENT -> BLPCToast.builder()
                    .fromPartyEvent(msg.getEventType(), msg.getPlayerName(), msg.getExtraInfo())
                    .build();
            case ClientNotify.KIND_CLAIM_FAILED -> BLPCToast.builder()
                    .fromClaimFailed(msg.getReason(), msg.getCurrent(), msg.getMax())
                    .build();
            default -> null;
        };
    }
}
