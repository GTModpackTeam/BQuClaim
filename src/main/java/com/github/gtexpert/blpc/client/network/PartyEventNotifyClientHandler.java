package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.client.gui.widget.BLPCToast;
import com.github.gtexpert.blpc.common.network.MessagePartyEventNotify;

/** Client-side handler for {@link MessagePartyEventNotify}. */
@SideOnly(Side.CLIENT)
public final class PartyEventNotifyClientHandler
                                                 implements IMessageHandler<MessagePartyEventNotify, IMessage> {

    @Override
    public IMessage onMessage(MessagePartyEventNotify msg, MessageContext ctx) {
        final String eventType = msg.getEventType();
        final String playerName = msg.getPlayerName();
        final String extraInfo = msg.getExtraInfo();
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BLPCToast toast = BLPCToast.builder()
                    .fromPartyEvent(eventType, playerName, extraInfo)
                    .build();
            Minecraft.getMinecraft().getToastGui().add(toast);
        });
        return null;
    }
}
