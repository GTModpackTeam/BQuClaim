package com.github.gtexpert.teamclaim.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.TeamClaimMod;
import com.github.gtexpert.teamclaim.common.chunk.ChunkManagerData;
import com.github.gtexpert.teamclaim.common.chunk.ClaimedChunkData;
import com.github.gtexpert.teamclaim.common.party.Party;
import com.github.gtexpert.teamclaim.common.party.PartyManagerData;

public class TeamClaimSaveHandler {

    public static final TeamClaimSaveHandler INSTANCE = new TeamClaimSaveHandler();

    private File dataDir;
    private File partiesDir;
    private File claimsDir;

    private TeamClaimSaveHandler() {}

    public void initWorldDir(MinecraftServer server) {
        File worldDir = server.getEntityWorld().getSaveHandler().getWorldDirectory();
        dataDir = new File(worldDir, "data/" + Tags.MODID);
        partiesDir = new File(dataDir, "parties");
        claimsDir = new File(dataDir, "claims");
        dataDir.mkdirs();
        partiesDir.mkdirs();
        claimsDir.mkdirs();
    }

    // --- Config ---

    public void loadConfig(PartyManagerData data) {
        File file = new File(dataDir, "config.dat");
        if (!file.exists()) return;
        try {
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
            data.readConfigNBT(nbt);
            TeamClaimMod.LOGGER.debug("Loaded config from {}", file);
        } catch (IOException e) {
            TeamClaimMod.LOGGER.error("Failed to load config", e);
        }
    }

    public void saveConfig(PartyManagerData data) {
        File file = new File(dataDir, "config.dat");
        try {
            NBTTagCompound nbt = new NBTTagCompound();
            data.writeConfigNBT(nbt);
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(file));
        } catch (IOException e) {
            TeamClaimMod.LOGGER.error("Failed to save config", e);
        }
    }

    // --- Parties (one file per party) ---

    public void loadParties(PartyManagerData data) {
        File[] files = partiesDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;
        for (File file : files) {
            try {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
                Party party = Party.fromNBT(nbt);
                data.addParty(party);
            } catch (IOException e) {
                TeamClaimMod.LOGGER.error("Failed to load party from {}", file, e);
            }
        }
        TeamClaimMod.LOGGER.debug("Loaded {} parties from {}", data.getAllParties().size(), partiesDir);
    }

    public void saveParties(PartyManagerData data) {
        File[] oldFiles = partiesDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (oldFiles != null) {
            for (File f : oldFiles) f.delete();
        }
        for (Party party : data.getAllParties()) {
            File file = new File(partiesDir, party.getPartyId() + ".dat");
            try {
                CompressedStreamTools.writeCompressed(party.toNBT(), new FileOutputStream(file));
            } catch (IOException e) {
                TeamClaimMod.LOGGER.error("Failed to save party {}", party.getPartyId(), e);
            }
        }
    }

    // --- Claims (per-party files) ---

    public void loadClaims(ChunkManagerData data) {
        File[] files = claimsDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;
        for (File file : files) {
            try {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
                NBTTagList list = nbt.getTagList("claims", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < list.tagCount(); i++) {
                    ClaimedChunkData claim = ClaimedChunkData.fromNBT(list.getCompoundTagAt(i));
                    data.setClaim(claim.x, claim.z, claim.ownerUUID, claim.ownerName, claim.partyName,
                            claim.isForceLoaded);
                }
            } catch (IOException e) {
                TeamClaimMod.LOGGER.error("Failed to load claims from {}", file, e);
            }
        }
        TeamClaimMod.LOGGER.debug("Loaded claims from {}", claimsDir);
    }

    public void saveClaims(ChunkManagerData chunkData, PartyManagerData partyData) {
        File[] oldFiles = claimsDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (oldFiles != null) {
            for (File f : oldFiles) f.delete();
        }

        Map<Integer, NBTTagList> partyLists = new HashMap<>();
        NBTTagList globalList = new NBTTagList();

        for (ClaimedChunkData claim : chunkData.getAllClaims()) {
            Party party = partyData.getPartyByPlayer(claim.ownerUUID);
            if (party != null) {
                partyLists.computeIfAbsent(party.getPartyId(), k -> new NBTTagList()).appendTag(claim.toNBT());
            } else {
                globalList.appendTag(claim.toNBT());
            }
        }

        saveClaimList(new File(claimsDir, "global.dat"), globalList);
        for (Map.Entry<Integer, NBTTagList> entry : partyLists.entrySet()) {
            saveClaimList(new File(claimsDir, entry.getKey() + ".dat"), entry.getValue());
        }
    }

    private void saveClaimList(File file, NBTTagList list) {
        if (list.tagCount() == 0) return;
        try {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setTag("claims", list);
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(file));
        } catch (IOException e) {
            TeamClaimMod.LOGGER.error("Failed to save claims to {}", file, e);
        }
    }

    // --- Full save/load ---

    public void loadAll(MinecraftServer server) {
        initWorldDir(server);
        ChunkManagerData.reset();
        PartyManagerData.reset();
        PartyManagerData partyData = PartyManagerData.getInstance();
        ChunkManagerData chunkData = ChunkManagerData.getInstance();
        loadConfig(partyData);
        loadParties(partyData);
        loadClaims(chunkData);
    }

    public void saveAll() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || dataDir == null) return;
        PartyManagerData partyData = PartyManagerData.getInstance();
        ChunkManagerData chunkData = ChunkManagerData.getInstance();
        saveConfig(partyData);
        saveParties(partyData);
        saveClaims(chunkData, partyData);
    }
}
