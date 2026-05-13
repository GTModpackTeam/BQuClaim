package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.client.gui.widget.BLPCToast;
import com.github.gtexpert.blpc.common.network.MessageClientNotify;
import com.github.gtexpert.blpc.common.party.RelationType;

/**
 * Client-side handler for {@link MessageClientNotify}. Dispatches by
 * {@link MessageClientNotify#getKind() kind} to the appropriate
 * {@link BLPCToast} configuration. Replaces the per-kind handlers
 * (chunk transit / party event / claim failed) — adding a new kind only
 * requires a new {@code case} arm here, no new wire ID.
 */
@SideOnly(Side.CLIENT)
public final class ClientNotifyClientHandler implements IMessageHandler<MessageClientNotify, IMessage> {

    @Override
    public IMessage onMessage(MessageClientNotify msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BLPCToast toast = buildToast(msg);
            if (toast != null) {
                Minecraft.getMinecraft().getToastGui().add(toast);
            }
        });
        return null;
    }

    private static BLPCToast buildToast(MessageClientNotify msg) {
        return switch (msg.getKind()) {
            case MessageClientNotify.KIND_CHUNK_TRANSIT -> BLPCToast.builder()
                    .fromTransit(parseRelation(msg.getRelationName()), msg.isEntered(), msg.getPlayerName())
                    .build();
            case MessageClientNotify.KIND_PARTY_EVENT -> BLPCToast.builder()
                    .fromPartyEvent(msg.getEventType(), msg.getPlayerName(), msg.getExtraInfo())
                    .build();
            case MessageClientNotify.KIND_CLAIM_FAILED -> BLPCToast.builder()
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
