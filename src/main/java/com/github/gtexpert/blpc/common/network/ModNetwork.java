package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.client.network.ClientPacketHandlers;
import com.github.gtexpert.blpc.common.network.party.PartyActionDispatcher;

/**
 * Network channel initialization. Messages use incrementing discriminator IDs.
 * <p>
 * The wire protocol assigns each message a stable discriminator. C→S handlers live in
 * {@code common.network.*} (no client-only references). All S→C handlers live in
 * {@code client.network.*} and are registered through {@link ClientPacketHandlers} on
 * the physical client only — on a dedicated server they are replaced with
 * {@link NoOpHandler} so the same discriminators remain valid for outbound sends.
 */
public class ModNetwork {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID);

    /** Wire protocol IDs. The order here is part of the on-wire contract — do not reorder. */
    private static final Class<? extends IMessage>[] CLIENT_BOUND_MESSAGES = clientBoundMessages();

    @SuppressWarnings("unchecked")
    private static Class<? extends IMessage>[] clientBoundMessages() {
        return new Class[] { MessageSyncClaims.class, MessageSyncAllClaims.class, MessageSyncConfig.class,
                MessagePartySync.class, MessageChunkTransitNotify.class, MessagePartyEventNotify.class,
                MessageClaimFailed.class };
    }

    public static void init() {
        int id = 0;

        // C→S: server handlers live in common.network and have no client-only references.
        INSTANCE.registerMessage(MessageClaimChunk.Handler.class, MessageClaimChunk.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PartyActionDispatcher.class, MessagePartyAction.class, id++, Side.SERVER);

        // S→C: handlers live in client.network and reference @SideOnly(CLIENT) classes
        // (Minecraft, IToast, etc.). Loading them on a dedicated server triggers the
        // SideTransformer and crashes class verification, so they are installed via the
        // ClientPacketHandlers SPI on the physical client only. On a server we register
        // a NoOpHandler under each discriminator so outbound sends still resolve.
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ClientPacketHandlers.installAll(INSTANCE, id);
        } else {
            for (Class<? extends IMessage> messageClass : CLIENT_BOUND_MESSAGES) {
                registerNoOp(messageClass, id++);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <REQ extends IMessage> void registerNoOp(Class<REQ> messageClass, int discriminator) {
        Class handlerClass = NoOpHandler.class;
        INSTANCE.registerMessage((Class<? extends IMessageHandler<REQ, IMessage>>) handlerClass, messageClass,
                discriminator, Side.CLIENT);
    }
}
