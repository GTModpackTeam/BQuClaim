package com.github.gtexpert.blpc.common.party;

/**
 * Enumerates the protection actions that can be configured per-party via trust levels.
 * <p>
 * Each action maps to one or more Forge events in
 * {@link com.github.gtexpert.blpc.core.ChunkProtectionHandler ChunkProtectionHandler}.
 * The party owner can set the minimum {@link TrustLevel} required for each action
 * through the Settings panel.
 */
public enum TrustAction {

    /**
     * Block breaking and placing.
     * <p>
     * Forge events: {@link net.minecraftforge.event.world.BlockEvent.BreakEvent BreakEvent},
     * {@link net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent EntityPlaceEvent},
     * {@link net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent FarmlandTrampleEvent}.
     */
    BLOCK_EDIT("blockEdit", TrustLevel.ALLY),
    /**
     * Right-click interactions with blocks and entities.
     * <p>
     * Forge events: {@link net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock RightClickBlock},
     * {@link net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract EntityInteract},
     * {@link net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific EntityInteractSpecific}.
     */
    BLOCK_INTERACT("blockInteract", TrustLevel.ALLY),
    /**
     * Attacking entities (players and mobs).
     * <p>
     * Forge event: {@link net.minecraftforge.event.entity.player.AttackEntityEvent AttackEntityEvent}.
     */
    ATTACK_ENTITY("attackEntity", TrustLevel.ALLY),
    /**
     * Using items (right-click item in hand) within claimed chunks.
     * <p>
     * Forge event: {@link net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem RightClickItem}.
     */
    USE_ITEM("useItem", TrustLevel.ALLY);

    private final String nbtKey;
    private final TrustLevel defaultLevel;

    TrustAction(String nbtKey, TrustLevel defaultLevel) {
        this.nbtKey = nbtKey;
        this.defaultLevel = defaultLevel;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public TrustLevel getDefaultLevel() {
        return defaultLevel;
    }

    public static TrustAction fromNbtKey(String key) {
        for (TrustAction action : values()) {
            if (action.nbtKey.equals(key)) return action;
        }
        return null;
    }
}
