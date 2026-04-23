package com.github.gtexpert.blpc.common.network;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.BLPCSaveHandler;
import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.ModLog;
import com.github.gtexpert.blpc.common.chunk.ChunkManagerData;
import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.TicketManager;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;
import com.github.gtexpert.blpc.common.party.PartyRole;

/** Sends initial sync packets (claims, config, parties) to newly connected players. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // Merge offline UUID -> online UUID if configured
        if (ModConfig.data.mergeOfflineOnlineData) {
            UUID onlineUUID = player.getUniqueID();
            UUID offlineUUID = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + player.getName()).getBytes(StandardCharsets.UTF_8));
            if (!onlineUUID.equals(offlineUUID)) {
                PartyManagerData pmData = PartyManagerData.getInstance();
                Party oldParty = pmData.getPartyByPlayer(offlineUUID);
                if (oldParty != null && pmData.getPartyByPlayer(onlineUUID) == null) {
                    PartyRole role = oldParty.getRole(offlineUUID);
                    if (role == null) role = PartyRole.MEMBER;
                    oldParty.removeMember(offlineUUID);
                    oldParty.addMember(onlineUUID, role);

                    ChunkManagerData chunkData = ChunkManagerData.getInstance();
                    chunkData.transferOwnership(offlineUUID, onlineUUID);

                    BLPCSaveHandler.INSTANCE.markDirty();
                    ModLog.MIGRATION.info("Merged offline UUID {} -> online UUID {} for player {}",
                            offlineUUID, onlineUUID, player.getName());
                    PartyProviderRegistry.get().syncToAll();
                }
            }
        }

        // Auto-create party if configured
        IPartyProvider activeProvider = PartyProviderRegistry.get();
        if (!activeProvider.hasNativeParty(player.getUniqueID())) {
            boolean isSingleplayer = player.getServer() != null && player.getServer().isSinglePlayer();
            boolean shouldCreate = isSingleplayer && ModConfig.party.autoCreatePartySingleplayer;
            if (shouldCreate) {
                if (activeProvider.createParty(player, player.getName())) {
                    activeProvider.syncToAll();
                }
                BLPCSaveHandler.INSTANCE.markDirty();
            }
        }

        // Re-force party chunks if this is the first member logging in after offline suppression
        if (!ModConfig.claims.allowOfflineChunkLoading) {
            Party party = PartyManagerData.getInstance().getPartyByPlayer(player.getUniqueID());
            if (party != null) {
                MinecraftServer server = player.getServer();
                if (server != null && party.countOnlineMembers(server) == 1) {
                    Set<UUID> memberIds = new HashSet<>(party.getMemberUUIDs());
                    for (ClaimedChunkData claim : ChunkManagerData.getInstance().getAllClaims()) {
                        if (claim.isForceLoaded && memberIds.contains(claim.ownerUUID)) {
                            for (WorldServer ws : server.worlds) {
                                TicketManager.forceChunk(ws, claim.x, claim.z, null);
                            }
                        }
                    }
                }
            }
        }

        ChunkManagerData data = ChunkManagerData.getInstance();

        ModNetwork.INSTANCE.sendTo(new MessageSyncAllClaims(data.serializeAll()), player);
        ModNetwork.INSTANCE.sendTo(
                new MessageSyncConfig(ModConfig.claims.maxClaimsPerPlayer, ModConfig.claims.maxForceLoadsPerPlayer),
                player);

        ModNetwork.INSTANCE.sendTo(
                new MessagePartySync(PartyProviderRegistry.get().serializeForClient()), player);
    }
}
