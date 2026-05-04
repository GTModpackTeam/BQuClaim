package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * No-op handler used on sides that should never actually receive a message but still need
 * the message class registered for sending. Avoids loading client-only handler bytecode
 * (e.g., toast UI references) on the dedicated server.
 */
public class NoOpHandler implements IMessageHandler<IMessage, IMessage> {

    @Override
    public IMessage onMessage(IMessage message, MessageContext ctx) {
        return null;
    }
}
