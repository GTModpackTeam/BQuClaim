package com.github.gtexpert.teamclaim.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.api.party.PartyProviderRegistry;
import com.github.gtexpert.teamclaim.common.ModConfig;
import com.github.gtexpert.teamclaim.common.chunk.ChunkManagerData;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ChunkManagerData data = ChunkManagerData.get(player.world);

        ModNetwork.INSTANCE.sendTo(new MessageSyncAllClaims(data.serializeAll()), player);
        ModNetwork.INSTANCE.sendTo(
                new MessageSyncConfig(ModConfig.maxClaimsPerPlayer, ModConfig.maxForceLoadsPerPlayer), player);

        ModNetwork.INSTANCE.sendTo(
                new MessageTeamSync(PartyProviderRegistry.get().serializeForClient()), player);
    }
}
