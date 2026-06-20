package com.github.gtexpert.blpc.client.network;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import com.github.gtexpert.blpc.common.network.message.ClientNotify;
import com.github.gtexpert.blpc.common.network.message.PartySync;
import com.github.gtexpert.blpc.common.network.message.SyncAllClaims;
import com.github.gtexpert.blpc.common.network.message.SyncClaims;
import com.github.gtexpert.blpc.common.network.message.SyncConfig;

/**
 * Side-aware installer for all S→C client handlers.
 * <p>
 * This class is intentionally NOT annotated {@code @SideOnly(CLIENT)} so it can be
 * referenced from common-side {@code ModNetwork.init()} without triggering the
 * SideTransformer on a dedicated server. The actual handler classes referenced below
 * via class literals (ldc) ARE {@code @SideOnly(CLIENT)}; they are only resolved at
 * runtime when {@link #installAll(SimpleNetworkWrapper, int)} actually executes — i.e.
 * only on the physical client.
 * <p>
 * The order here must match {@code ModNetwork.CLIENT_BOUND_MESSAGES} on the server
 * side so server-side {@link com.github.gtexpert.blpc.common.network.NoOpHandler}
 * registration and client-side real registration share the same discriminators.
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    /**
     * Registers all S→C handlers on the given channel, starting at {@code firstId}
     * and consuming sequential discriminator IDs in a stable order.
     */
    public static void installAll(SimpleNetworkWrapper channel, int firstId) {
        int id = firstId;
        channel.registerMessage(SyncClaimsClientHandler.class, SyncClaims.class,
                id++, Side.CLIENT);
        channel.registerMessage(SyncAllClaimsClientHandler.class, SyncAllClaims.class,
                id++, Side.CLIENT);
        channel.registerMessage(SyncConfigClientHandler.class, SyncConfig.class,
                id++, Side.CLIENT);
        channel.registerMessage(PartySyncClientHandler.class, PartySync.class,
                id++, Side.CLIENT);
        channel.registerMessage(ClientNotifyClientHandler.class, ClientNotify.class,
                id++, Side.CLIENT);
    }
}
