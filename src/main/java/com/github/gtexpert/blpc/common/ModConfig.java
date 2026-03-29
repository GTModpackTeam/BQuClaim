package com.github.gtexpert.blpc.common;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.github.gtexpert.blpc.Tags;

@Config(modid = Tags.MODID, name = Tags.MODID + "/" + Tags.MODID)
public class ModConfig {

    @Config.LangKey("config.blpc.claims")
    public static final Claims claims = new Claims();

    @Config.LangKey("config.blpc.party")
    public static final Party party = new Party();

    @Config.LangKey("config.blpc.server_party")
    public static final ServerParty serverParty = new ServerParty();

    @Config.LangKey("config.blpc.data")
    public static final Data data = new Data();

    public static class Claims {

        @Config.Name("Max Claims Per Player")
        @Config.Comment("Maximum chunks a player can claim.")
        @Config.RangeInt(min = 0, max = 10000)
        public int maxClaimsPerPlayer = 1000;

        @Config.Name("Max Force Loads Per Player")
        @Config.Comment("Maximum chunks a player can force load.")
        @Config.RangeInt(min = 0, max = 10000)
        public int maxForceLoadsPerPlayer = 64;
    }

    public static class Party {

        @Config.Name("Auto Create Party (Singleplayer)")
        @Config.Comment("Automatically create a party named after the player in singleplayer.")
        public boolean autoCreatePartySingleplayer = false;
    }

    public static class ServerParty {

        @Config.Name("Enable")
        @Config.Comment("Automatically create a shared party on server start.")
        public boolean enabled = false;

        @Config.Name("Party Name")
        @Config.Comment("Name for the auto-created server party.")
        public String name = "Server";

        @Config.Name("Free to Join")
        @Config.Comment("Enable free-to-join (auto-join) on the server party.")
        public boolean freeToJoin = true;

        @Config.Name("Owner")
        @Config.Comment("Player name who owns the server party. Leave empty for server-owned.")
        public String owner = "";

        @Config.Name("Moderators")
        @Config.Comment("Player names to assign as moderators (ADMIN role).")
        public String[] moderators = {};
    }

    public static class Data {

        @Config.Name("Merge Offline/Online Data")
        @Config.Comment("Merge chunk data when switching between offline and online mode.")
        public boolean mergeOfflineOnlineData = true;
    }

    // Sync config values immediately when changed via in-game settings GUI
    @Mod.EventBusSubscriber(modid = Tags.MODID)
    private static class EventHandler {

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Tags.MODID)) {
                ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
