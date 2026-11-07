package danger.orespawn.dangerzone.world;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.item.DimensionStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/** The three dimensions and what runs in them: the Kraken King's castles, the warp in, and Doom's sand rain. */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class DzPlaces {
    public static final ResourceKey<Level> KRAKEN_REALM = key("kraken_realm");
    public static final ResourceKey<Level> DRAGONS_LAIR = key("dragons_lair");
    public static final ResourceKey<Level> DOOM = key("doom");

    /** One castle per 128 chunks, at chunk 29 of the grid. floorMod: DangerZone's % leaves negative coordinates without any. */
    private static final int CASTLE_GRID = 128;
    private static final int CASTLE_AT = 29;
    private static final int DOOM_SURFACE = 50;
    private static final Queue<Runnable> NEXT_TICK = new ConcurrentLinkedQueue<>();

    private DzPlaces() {
    }

    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, OreSpawnDangerZone.id(name));
    }

    /** Which castles have been built, so that each goes up once. */
    public static final class Castles extends SavedData {
        private static final String NAME = "orespawn_dangerzone_castles";
        final Set<Long> built = new HashSet<>();
        /** Builds begun and not finished, as block positions: they start over if the server stopped part way. */
        final Set<Long> unfinished = new HashSet<>();

        static Castles get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(Castles::new, Castles::load), NAME);
        }

        private static Castles load(CompoundTag tag, HolderLookup.Provider registries) {
            Castles castles = new Castles();
            for (long pos : tag.getLongArray("Built")) {
                castles.built.add(pos);
            }
            for (long pos : tag.getLongArray("Unfinished")) {
                castles.unfinished.add(pos);
            }
            return castles;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Built", new LongArrayTag(this.built.stream().mapToLong(Long::longValue).toArray()));
            tag.put("Unfinished", new LongArrayTag(this.unfinished.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }
    }

    public static boolean isCastleChunk(int chunkX, int chunkZ) {
        return Math.floorMod(chunkX, CASTLE_GRID) == CASTLE_AT && Math.floorMod(chunkZ, CASTLE_GRID) == CASTLE_AT;
    }

    /** The nearest castle's chunk to a block position. */
    public static ChunkPos nearestCastle(double x, double z) {
        int cx = Math.round((float) ((x / 16.0 - CASTLE_AT) / CASTLE_GRID)) * CASTLE_GRID + CASTLE_AT;
        int cz = Math.round((float) ((z / 16.0 - CASTLE_AT) / CASTLE_GRID)) * CASTLE_GRID + CASTLE_AT;
        return new ChunkPos(cx, cz);
    }

    /** Puts up the castle at this chunk if it isn't there yet. Returns the builder, or null if it already stands. */
    public static DzBuilder buildCastle(ServerLevel level, ChunkPos chunk) {
        Castles castles = Castles.get(level);
        if (!castles.built.add(chunk.toLong())) {
            return null;
        }
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int y = 70;
        while (y < 80 && !level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            y++;
        }
        return runCastle(level, new BlockPos(x, y, z));
    }

    private static DzBuilder runCastle(ServerLevel level, BlockPos corner) {
        Castles castles = Castles.get(level);
        long key = corner.asLong();
        castles.unfinished.add(key);
        castles.setDirty();
        DzBuilder builder = new DzBuilder(level);
        DzDungeons.makeKrakenKingCastle(builder, corner.getX(), corner.getY(), corner.getZ());
        builder.start(() -> {
            castles.unfinished.remove(key);
            castles.setDirty();
        });
        return builder;
    }

    private static DzBuilder runLair(ServerLevel level, BlockPos torch) {
        Castles castles = Castles.get(level);
        long key = torch.asLong();
        castles.unfinished.add(key);
        castles.setDirty();
        DzBuilder builder = new DzBuilder(level);
        DzDungeons.makeDargonsLair(builder, torch.getX(), torch.getY(), torch.getZ());
        builder.start(() -> {
            castles.unfinished.remove(key);
            castles.setDirty();
        });
        return builder;
    }

    /** A build's creatures come last, so one cut off by a stop has placed none, and running it again from the top is safe. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel realm = event.getServer().getLevel(KRAKEN_REALM);
        if (realm != null) {
            for (long pos : List.copyOf(Castles.get(realm).unfinished)) {
                runCastle(realm, BlockPos.of(pos));
            }
        }
        ServerLevel lair = event.getServer().getLevel(DRAGONS_LAIR);
        if (lair != null) {
            for (long pos : List.copyOf(Castles.get(lair).unfinished)) {
                runLair(lair, BlockPos.of(pos));
            }
        }
    }

    /**
     * Runs work at the top of the next tick. For anything that may load chunks or set blocks from inside a chunk load or
     * a block's placement. Not server.execute: on the server thread that runs on the spot, and a build started in the
     * middle of a chunk load waits for ever on chunks that can't finish until the load returns.
     */
    public static void nextTick(Runnable work) {
        NEXT_TICK.add(work);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Runnable work;
        while ((work = NEXT_TICK.poll()) != null) {
            work.run();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NEXT_TICK.clear();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == KRAKEN_REALM
                && isCastleChunk(event.getChunk().getPos().x, event.getChunk().getPos().z)) {
            ChunkPos chunk = event.getChunk().getPos();
            nextTick(() -> buildCastle(level, chunk));
        }
    }

    /** Sends the player to the Kraken King's realm, within sight of the nearest castle, which gets built if need be. */
    public static void warpToRealm(ServerPlayer player) {
        ServerLevel realm = player.server.getLevel(KRAKEN_REALM);
        if (realm == null || player.level() == realm) {
            return;
        }
        ChunkPos castle = nearestCastle(player.getX(), player.getZ());
        realm.getChunk(castle.x, castle.z);
        buildCastle(realm, castle);
        // Keeps clear of the walls: the castle runs 53 blocks along x from this corner.
        int x = castle.getMinBlockX() - 24;
        int z = castle.getMinBlockZ() + 21;
        realm.getChunk(x >> 4, z >> 4);
        DimensionStaffItem.jumpTo(player, realm, x, realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1, z);
    }

    /** Doom's sand rain: a hole near a player gets sand dropped into it from the top of the sky, so the desert heals. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != DOOM || level.players().isEmpty()) {
            return;
        }
        if (level.random.nextInt(3) != 0) {
            return;
        }
        ServerPlayer player = level.players().get(level.random.nextInt(level.players().size()));
        int x = player.getBlockX() + level.random.nextInt(128) - level.random.nextInt(128);
        int z = player.getBlockZ() + level.random.nextInt(128) - level.random.nextInt(128);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        BlockPos hole = new BlockPos(x, DOOM_SURFACE - 1, z);
        if (level.getBlockState(hole).isAir() && level.getEntitiesOfClass(FallingBlockEntity.class, player.getBoundingBox().inflate(128.0, 256.0, 128.0)).size() < 24) {
            FallingBlockEntity.fall(level, new BlockPos(x, level.getMaxBuildHeight() - 2, z), Blocks.SAND.defaultBlockState()).dropItem = false;
        }
    }

    /** The carve and the castle behind an Extreme Torch: a platform at once, so nobody falls while the rest happens. */
    public static DzBuilder startLair(ServerLevel level, BlockPos torch) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.setBlock(torch.offset(dx, 0, dz), ModContent.DARGON_STONE_DARK.get().defaultBlockState(), 2);
                for (int dy = 1; dy <= 4; dy++) {
                    level.setBlock(torch.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(torch, 12.0)) {
                player.teleportTo(torch.getX() + 0.5, torch.getY() + 1.0, torch.getZ() + 0.5);
            }
        }
        return runLair(level, torch);
    }
}
