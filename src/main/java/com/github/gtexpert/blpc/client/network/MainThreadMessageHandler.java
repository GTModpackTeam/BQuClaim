package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Base for S→C handlers that apply their payload on the client's main thread.
 * Netty delivers {@link #onMessage} off-thread; every BLPC client handler needs
 * to hop onto the main thread before touching Minecraft/cache state, so this
 * base does the scheduling once instead of repeating it per handler.
 */
@SideOnly(Side.CLIENT)
public abstract class MainThreadMessageHandler<REQ extends IMessage> implements IMessageHandler<REQ, IMessage> {

    @Override
    public final IMessage onMessage(REQ msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> handleOnMainThread(msg));
        return null;
    }

    protected abstract void handleOnMainThread(REQ msg);
}
