package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DangerZone's swarms: every so often a horde crosses the map past a random player, heading the same way the whole
 * time, and is gone a minute or so later. It warns you in chat first, ominously. What comes depends on where you are
 * and the time of day, with port creatures where DangerZone's own (Bullet Bats, Geese) don't exist here.
 */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class Swarms {
    private static final String TAG = "orespawn_dangerzone_swarm";
    private static final int FADE_AFTER = 900;
    private static final int GONE_AFTER = 1800;
    private static final int FLYING_START = 160;
    private static final int WALKING_START = 100;
    private static final List<String> WARNINGS = List.of("swarm.orespawn_dangerzone.0", "swarm.orespawn_dangerzone.1",
            "swarm.orespawn_dangerzone.2", "swarm.orespawn_dangerzone.3", "swarm.orespawn_dangerzone.4");

    /** dimension -> { by day, by night }, each a choice of two. Empty means nothing comes at that hour. */
    private static final Map<String, String[][]> TABLE = Map.of(
            "orespawn:mining", new String[][]{{"orespawn:bee", "orespawn:mosquito"}, {"orespawn:mosquito"}},
            "orespawn:utopia", new String[][]{{"orespawn:butterfly", "orespawn:dragonfly"}, {}},
            "orespawn:village", new String[][]{{}, {"orespawn:robot_5", "orespawn:robot_1"}},
            "orespawn:islands", new String[][]{{}, {"orespawn:pitch_black", "orespawn:terrible_terror"}},
            "orespawn:crystal", new String[][]{{"orespawn:mantis"}, {"orespawn:rotator"}},
            "minecraft:overworld", new String[][]{{"orespawn:butterfly"}, {"orespawn:mosquito"}});

    private record Member(Mob mob, int born, Vec3 heading, boolean flies) {
    }

    private static final List<Member> ACTIVE = new ArrayList<>();

    private Swarms() {
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int minutes = DzConfig.SWARM_AVERAGE_MINUTES.get();
        if (minutes > 0 && ACTIVE.isEmpty() && server.overworld().random.nextInt(minutes * 60 * 20) == 0) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (!players.isEmpty()) {
                start(players.get(server.overworld().random.nextInt(players.size())));
            }
        }
        steer(server);
    }

    /** What would swarm at this player right now, or null. */
    @Nullable
    public static EntityType<?> choice(ServerLevel level, RandomSource random) {
        String[][] entry = TABLE.get(level.dimension().location().toString());
        if (entry == null) {
            return null;
        }
        String[] options = entry[level.isDay() ? 0 : 1];
        if (options.length == 0) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(options[random.nextInt(options.length)]));
    }

    /** Sends one swarm at the player if their dimension and hour have one. Returns how many came. */
    public static int start(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        EntityType<?> type = choice(level, random);
        if (type == null) {
            return 0;
        }
        Entity probe = type.create(level);
        boolean flies = probe != null && (probe.isNoGravity() || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:mosquito"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:butterfly"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:bee"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:dragonfly"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:mantis"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:pitch_black"))
                || type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:terrible_terror")));
        if (probe != null) {
            probe.discard();
        }
        int start = flies ? FLYING_START : WALKING_START;
        Vec3 heading = new Vec3(1.0, 0.0, 1.0).normalize();
        int size = DzConfig.SWARM_SIZE.get();
        int came = 0;
        for (int i = 0; i < size; i++) {
            int x = player.getBlockX() - start + random.nextInt(12) - random.nextInt(12);
            int z = player.getBlockZ() - start + random.nextInt(12) - random.nextInt(12);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + (flies ? 8 + random.nextInt(12) : 0);
            if (type.create(level) instanceof Mob mob) {
                mob.moveTo(x + 0.5, y, z + 0.5, -45.0f, 0.0f);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
                mob.getPersistentData().putBoolean(TAG, true);
                // They start far enough off to count as forgotten: the timer below is what ends them.
                mob.setPersistenceRequired();
                if (level.addFreshEntity(mob)) {
                    ACTIVE.add(new Member(mob, level.getServer().getTickCount(), heading, flies));
                    came++;
                }
            }
        }
        if (came > 0) {
            player.sendSystemMessage(Component.translatable(WARNINGS.get(random.nextInt(WARNINGS.size()))));
        }
        return came;
    }

    private static void steer(MinecraftServer server) {
        int now = server.getTickCount();
        ACTIVE.removeIf(member -> {
            Mob mob = member.mob();
            if (!mob.isAlive() || mob.isRemoved()) {
                return true;
            }
            int age = now - member.born();
            if (age >= GONE_AFTER || (age >= FADE_AFTER && mob.getRandom().nextInt(30) == 0)) {
                mob.discard();
                return true;
            }
            double speed = member.flies() ? 0.45 : 0.28;
            double up = mob.getDeltaMovement().y;
            if (member.flies()) {
                int ground = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mob.getBlockX(), mob.getBlockZ());
                up = mob.getY() < ground + 6 ? 0.15 : mob.getY() > ground + 22 ? -0.1 : 0.0;
            }
            mob.setDeltaMovement(member.heading().x * speed, up, member.heading().z * speed);
            mob.setYRot(-45.0f);
            mob.setYBodyRot(-45.0f);
            mob.hasImpulse = true;
            return false;
        });
    }

    /** The tracker holds entities, and in singleplayer the same game can go on to open another world. */
    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    /** Whether the swarm tracker has anyone. Public for the tests. */
    public static int activeCount() {
        return ACTIVE.size();
    }

    /** A swarm doesn't survive a restart: anything loaded back from disk with the mark is gone. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().getPersistentData().getBoolean(TAG)) {
            event.setCanceled(true);
        }
    }
}
