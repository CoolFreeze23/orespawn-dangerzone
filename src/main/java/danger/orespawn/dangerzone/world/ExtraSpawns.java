package danger.orespawn.dangerzone.world;

import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Creatures the game's own spawn loop can't place. It only tries positions near the ground, where a sixteen-block UFO
 * or a twenty-three-block Dargon almost never fits, and the port's Kraken, Attack Squid and Terrible Terror each have a
 * spawn rule of the port's own that the new dimensions don't meet. So these are rolled for here, near each player.
 */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class ExtraSpawns {
    private static final int ROLL_EVERY = 400;
    private static final int TRIES = 8;
    private static final int NEAREST = 32;
    private static final int FURTHEST = 80;
    private static final double COUNT_WITHIN = 160.0;

    public enum Where { SKY, LAND, WATER }

    /** With a roll every 20 seconds, oneIn 60 is about one every 20 minutes. Public for the tests. */
    public record Entry(ResourceKey<Biome> biome, ResourceLocation type, int oneIn, int cap, Where where) {
    }

    private static final ResourceKey<Biome> CHAOS = biome("orespawn", "chaos_biome");
    private static final ResourceKey<Biome> VILLAGE = biome("orespawn", "village_biome");
    private static final ResourceKey<Biome> REALM = biome(OreSpawnDangerZone.MOD_ID, "kraken_realm");
    private static final ResourceKey<Biome> LAIR = biome(OreSpawnDangerZone.MOD_ID, "dragons_lair");

    private static final List<Entry> ENTRIES = List.of(
            new Entry(CHAOS, OreSpawnDangerZone.id("ufo"), 60, 1, Where.SKY),
            new Entry(VILLAGE, OreSpawnDangerZone.id("ufo"), 60, 1, Where.SKY),
            new Entry(CHAOS, OreSpawnDangerZone.id("dargon"), 180, 1, Where.SKY),
            new Entry(REALM, port("kraken"), 30, 2, Where.SKY),
            new Entry(REALM, port("attack_squid"), 3, 6, Where.WATER),
            new Entry(LAIR, port("terrible_terror"), 2, 6, Where.LAND));

    private ExtraSpawns() {
    }

    private static ResourceKey<Biome> biome(String namespace, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static ResourceLocation port(String path) {
        return ResourceLocation.fromNamespaceAndPath("orespawn", path);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ROLL_EVERY != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                continue;
            }
            for (Entry entry : entriesAt(level, player.blockPosition())) {
                if (level.random.nextInt(entry.oneIn()) == 0) {
                    spawnNear(level, player, entry);
                }
            }
        }
    }

    /** What can turn up at this spot. Public for the tests. */
    public static List<Entry> entriesAt(ServerLevel level, BlockPos pos) {
        Holder<Biome> here = level.getBiome(pos);
        return ENTRIES.stream().filter(entry -> here.is(entry.biome())).toList();
    }

    /** Puts one somewhere around the player if there's room for it and not too many about already. Public for the tests. */
    public static boolean spawnNear(ServerLevel level, ServerPlayer player, Entry entry) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.type());
        if (level.getEntities(type, player.getBoundingBox().inflate(COUNT_WITHIN), Entity::isAlive).size() >= entry.cap()) {
            return false;
        }
        RandomSource random = level.random;
        for (int tries = 0; tries < TRIES; tries++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = NEAREST + random.nextInt(FURTHEST - NEAREST);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            // Only where creatures are running: elsewhere one would stand frozen, and the cap above couldn't see it.
            if (!level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            boolean water = level.getFluidState(new BlockPos(x, top - 1, z)).is(FluidTags.WATER);
            int y;
            switch (entry.where()) {
                case SKY -> y = top + 10 + random.nextInt(20);
                case LAND -> {
                    if (water) {
                        continue;
                    }
                    y = top;
                }
                default -> {
                    if (!water) {
                        continue;
                    }
                    y = top - 1;
                }
            }
            if (!level.noCollision(type.getSpawnAABB(x + 0.5, y, z + 0.5)) || !(type.create(level) instanceof Mob mob)) {
                continue;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0f, 0.0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, null);
            return level.addFreshEntity(mob);
        }
        return false;
    }
}
