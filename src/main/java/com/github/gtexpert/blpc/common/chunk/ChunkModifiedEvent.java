package com.github.gtexpert.blpc.common.chunk;

import java.util.UUID;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class ChunkModifiedEvent extends Event {

    private final int chunkX;
    private final int chunkZ;
    private final UUID ownerUUID;

    protected ChunkModifiedEvent(int chunkX, int chunkZ, UUID ownerUUID) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.ownerUUID = ownerUUID;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public abstract static class Pre extends ChunkModifiedEvent {

        protected Pre(int chunkX, int chunkZ, UUID ownerUUID) {
            super(chunkX, chunkZ, ownerUUID);
        }

        @Cancelable
        public static class Claim extends Pre {

            public Claim(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        @Cancelable
        public static class Unclaim extends Pre {

            public Unclaim(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        @Cancelable
        public static class ForceLoad extends Pre {

            public ForceLoad(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        @Cancelable
        public static class Unforce extends Pre {

            public Unforce(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }
    }

    public abstract static class Post extends ChunkModifiedEvent {

        protected Post(int chunkX, int chunkZ, UUID ownerUUID) {
            super(chunkX, chunkZ, ownerUUID);
        }

        public static class Claim extends Post {

            public Claim(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        public static class Unclaim extends Post {

            public Unclaim(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        public static class ForceLoad extends Post {

            public ForceLoad(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }

        public static class Unforce extends Post {

            public Unforce(int chunkX, int chunkZ, UUID ownerUUID) {
                super(chunkX, chunkZ, ownerUUID);
            }
        }
    }
}
