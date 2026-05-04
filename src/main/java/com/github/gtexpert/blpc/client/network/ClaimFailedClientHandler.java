package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.client.gui.widget.BLPCToast;
import com.github.gtexpert.blpc.common.network.MessageClaimFailed;

/** Client-side handler for {@link MessageClaimFailed}. */
@SideOnly(Side.CLIENT)
public final class ClaimFailedClientHandler implements IMessageHandler<MessageClaimFailed, IMessage> {

    @Override
    public IMessage onMessage(MessageClaimFailed msg, MessageContext ctx) {
        final String reason = msg.getReason();
        final int current = msg.getCurrent();
        final int max = msg.getMax();
        Minecraft.getMinecraft().addScheduledTask(() -> {
            BLPCToast toast = BLPCToast.builder()
                    .fromClaimFailed(reason, current, max)
                    .build();
            Minecraft.getMinecraft().getToastGui().add(toast);
        });
        return null;
    }
}
