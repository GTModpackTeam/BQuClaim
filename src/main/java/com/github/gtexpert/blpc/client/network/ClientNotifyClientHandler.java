package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
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
public final class ClientNotifyClientHandler implements IMessageHandler<ClientNotify, IMessage> {

    @Override
    public IMessage onMessage(ClientNotify msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BLPCToast toast = buildToast(msg);
            if (toast != null) {
                Minecraft.getMinecraft().getToastGui().add(toast);
            }
        });
        return null;
    }

    private static BLPCToast buildToast(ClientNotify msg) {
        return switch (msg.getKind()) {
            case ClientNotify.KIND_CHUNK_TRANSIT -> BLPCToast.builder()
                    .fromTransit(parseRelation(msg.getRelationName()), msg.isEntered(), msg.getPlayerName())
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

    private static RelationType parseRelation(String name) {
        try {
            return RelationType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return RelationType.NONE;
        }
    }
}
