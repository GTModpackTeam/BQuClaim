package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.client.gui.widget.BLPCToast;
import com.github.gtexpert.blpc.common.network.MessageChunkTransitNotify;
import com.github.gtexpert.blpc.common.party.RelationType;

/**
 * Client-side handler for {@link MessageChunkTransitNotify}. References client-only
 * UI types ({@link BLPCToast}/{@code IToast}) so this class must never be loaded on a
 * dedicated server — registration is gated by {@link ClientPacketHandlers}.
 */
@SideOnly(Side.CLIENT)
public final class ChunkTransitNotifyClientHandler
                                                   implements IMessageHandler<MessageChunkTransitNotify, IMessage> {

    @Override
    public IMessage onMessage(MessageChunkTransitNotify msg, MessageContext ctx) {
        final RelationType relation = parseRelation(msg.getRelationName());
        final boolean entered = msg.isEntered();
        final String playerName = msg.getPlayerName();
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BLPCToast toast = BLPCToast.builder()
                    .fromTransit(relation, entered, playerName)
                    .build();
            Minecraft.getMinecraft().getToastGui().add(toast);
        });
        return null;
    }

    private static RelationType parseRelation(String name) {
        try {
            return RelationType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return RelationType.NONE;
        }
    }
}
