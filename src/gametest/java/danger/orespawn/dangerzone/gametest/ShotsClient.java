package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.item.DimensionStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.List;

/** Photographs every creature from two sides in a fresh flat world, in a hidden window, then exits. */
public final class ShotsClient {
    private record Subject(String name, EntityType<?> type, double distance, double height) {
    }

    private static final List<Subject> SUBJECTS = new ArrayList<>();
    private static final float[] YAWS = {35.0f, 200.0f};
    private static int subject;
    private static int view;
    private static int wait;
    private static boolean started;
    private static boolean placed;
    private static int liveStep;
    private static final int LIVE_STEPS = 7;
    private static final int LIVE_EVERY = 100;
    private static danger.orespawn.dangerzone.entity.TowerFlag flag;
    private static danger.orespawn.dangerzone.entity.Ufo ufo;
    private static danger.orespawn.dangerzone.entity.Volcano volcano;
    private static boolean staffChecked;
    private static String pendingShot;
    private static Entity current;

    private ShotsClient() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            // A fresh install opens the accessibility screen rather than the title screen; anything past loading will do.
            if (mc.getOverlay() == null && mc.screen != null) {
                started = true;
                SUBJECTS.add(new Subject("nastysaurus_prime", ModContent.NASTYSAURUS_PRIME.get(), 13.0, 3.0));
                SUBJECTS.add(new Subject("needlenose", ModContent.NEEDLENOSE.get(), 5.0, 1.0));
                SUBJECTS.add(new Subject("nurse_bug", ModContent.NURSE_BUG.get(), 8.0, 2.0));
                SUBJECTS.add(new Subject("ant_queen", ModContent.ANT_QUEEN.get(), 26.0, 5.0));
                SUBJECTS.add(new Subject("bloodlust_ant", ModContent.BLOODLUST_ANT.get(), 7.0, 1.5));
                SUBJECTS.add(new Subject("tower_flag", ModContent.TOWER_FLAG.get(), 16.0, 5.0));
                SUBJECTS.add(new Subject("nope", ModContent.NOPE.get(), 26.0, 4.0));
                SUBJECTS.add(new Subject("baby_nope", ModContent.BABY_NOPE.get(), 4.0, 0.5));
                SUBJECTS.add(new Subject("princess_teen", ModContent.PRINCESS_TEEN.get(), 12.0, 2.0));
                SUBJECTS.add(new Subject("princess_young_adult", ModContent.PRINCESS_YOUNG_ADULT.get(), 34.0, 6.0));
                SUBJECTS.add(new Subject("ufo", ModContent.UFO.get(), 36.0, 2.0));
                SUBJECTS.add(new Subject("ufo_carrier", ModContent.UFO_CARRIER.get(), 13.0, 1.0));
                SUBJECTS.add(new Subject("ufo_fighter", ModContent.UFO_FIGHTER.get(), 7.0, 1.0));
                SUBJECTS.add(new Subject("martian", ModContent.MARTIAN.get(), 4.0, 1.0));
                // one of this mod's creatures and one of the port's, each drawn through its own renderer
                SUBJECTS.add(new Subject("trophy_nope", ModContent.TROPHY_ENTITY.get(), 2.0, 0.4));
                SUBJECTS.add(new Subject("trophy_port", ModContent.TROPHY_ENTITY.get(), 2.0, 0.4));
                mc.options.pauseOnLostFocus = false;
                mc.options.hideGui = true;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                String world = "shots" + System.currentTimeMillis();
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel(world,
                        new LevelSettings(world, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(1L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
            }
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (subject >= SUBJECTS.size()) {
            if (!staffChecked) {
                staffChecked = true;
                mc.getSingleplayerServer().execute(ShotsClient::walkTheRing);
                wait = 200;
                return;
            }
            if (liveStep < LIVE_STEPS) {
                final int step = liveStep++;
                mc.getSingleplayerServer().execute(() -> live(step));
                wait = LIVE_EVERY;
                return;
            }
            System.out.println("[DZSHOTS] done");
            mc.stop();
            return;
        }
        Subject s = SUBJECTS.get(subject);
        var server = mc.getSingleplayerServer();
        if (!placed) {
            placed = true;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000L);
                if (current != null) {
                    current.discard();
                }
                current = s.type().create(level);
                current.moveTo(0.5 + subject * 100.0, -60.0, 0.5, 0.0f, 0.0f);
                if (current instanceof Mob mob) {
                    mob.setNoAi(true);
                    mob.setPersistenceRequired();
                }
                if (current instanceof danger.orespawn.dangerzone.entity.Trophy trophy) {
                    trophy.setSubject(s.name().equals("trophy_nope") ? "orespawn_dangerzone:nope" : "orespawn:godzilla");
                }
                level.getChunk(new BlockPos((int) current.getX(), 0, 0));
                level.addFreshEntity(current);
                server.getPlayerList().getPlayers().get(0).setGameMode(GameType.SPECTATOR);
            });
            wait = 20;
            return;
        }
        if (view >= YAWS.length) {
            subject++;
            view = 0;
            placed = false;
            return;
        }
        double radians = Math.toRadians(YAWS[view]);
        double targetX = 0.5 + subject * 100.0;
        double targetY = -60.0 + s.height();
        double targetZ = 0.5;
        double x = targetX + Math.sin(radians) * s.distance();
        double y = targetY + s.distance() * 0.25;
        double z = targetZ + Math.cos(radians) * s.distance();
        double dx = targetX - x;
        double dy = targetY - (y + 1.62);
        double dz = targetZ - z;
        final float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        final float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.teleportTo(server.overworld(), x, y, z, lookYaw, lookPitch);
        });
        pendingShot = "dz_" + s.name() + "_" + view + ".png";
        view++;
        wait = 40;
    }

    /** Everything running for real, unposed, beside a survival player in the overworld: what photos can't show. */
    private static void live(int step) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        ServerLevel level = server.overworld();
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        double x = 3000.5;
        double ground = -60.0;
        if (step == 0) {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, server);
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.teleportTo(level, x, ground, 0.5, 0.0f, 0.0f);
            flag = ModContent.TOWER_FLAG.get().create(level);
            // Further from the player than anything in the wave will notice, so what it does is about the flag.
            flag.moveTo(x, ground, 90.5, 0.0f, 0.0f);
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            flag.addAdditionalSaveData(saved);
            // Partway up the ladder, among the creatures that really fight.
            saved.putInt("Next", danger.orespawn.dangerzone.entity.TowerFlag.ladder().size() / 2);
            flag.readAdditionalSaveData(saved);
            level.addFreshEntity(flag);
            flag.nextWave(level);
            ufo = ModContent.UFO.get().create(level);
            ufo.moveTo(x, ground + 30.0, -30.5, 0.0f, 0.0f);
            level.addFreshEntity(ufo);
            volcano = ModContent.VOLCANO.get().create(level);
            volcano.moveTo(x + 60.0, ground, 0.5, 0.0f, 0.0f);
            level.addFreshEntity(volcano);
            level.setDayTime(18000L);
            int came = danger.orespawn.dangerzone.entity.Swarms.start(player);
            System.out.println("[DZSHOTS] live swarm came=" + came);
            return;
        }
        var ladder = danger.orespawn.dangerzone.entity.TowerFlag.ladder();
        var waveType = ladder.get(Math.max(0, flag.rung() - 1));
        var wave = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, flag.getBoundingBox().inflate(64.0), m -> m.getType() == waveType);
        long onFlag = wave.stream().filter(m -> m.getTarget() == flag).count();
        long onPlayer = wave.stream().filter(m -> m.getTarget() == player).count();
        double nearest = wave.stream().mapToDouble(m -> m.distanceTo(flag)).min().orElse(-1.0);
        System.out.println("[DZSHOTS] live wave " + BuiltInRegistries.ENTITY_TYPE.getKey(waveType) + " n=" + wave.size() + " onFlag=" + onFlag + " onPlayer=" + onPlayer
                + " nearest=" + String.format("%.1f", nearest) + " goals=" + wave.stream().findFirst().map(m -> m.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName()).limit(6).toList().toString()).orElse("-"));
        var box = player.getBoundingBox().inflate(96.0);
        long fighters = level.getEntitiesOfClass(danger.orespawn.dangerzone.entity.UfoFighter.class, box).size();
        long carriers = level.getEntitiesOfClass(danger.orespawn.dangerzone.entity.UfoCarrier.class, box).size();
        long martians = level.getEntitiesOfClass(danger.orespawn.dangerzone.entity.Martian.class, box).size();
        long blocks = level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, box).size();
        System.out.println(String.format("[DZSHOTS] live t=%d flagHealth=%.0f/%.0f rung=%d ufoY=%.1f fighters=%d carriers=%d martians=%d thrownBlocks=%d volcanoY=%.0f swarm=%d tps~%.1f",
                step * LIVE_EVERY, flag.getHealth(), flag.getMaxHealth(), flag.rung(), ufo.getY(), fighters, carriers, martians, blocks, volcano.getY(),
                danger.orespawn.dangerzone.entity.Swarms.activeCount(), Math.min(20.0, 1000.0 / Math.max(1.0, server.getCurrentSmoothedTickTime()))));
    }

    /** The Dimension Staff through every dimension of a real world, the player's real landing logged for each. */
    private static void walkTheRing() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.CREATIVE);
        var home = player.serverLevel().dimension();
        ServerLevel next = player.serverLevel();
        int bad = 0;
        do {
            next = DimensionStaffItem.neighbour(server, next.dimension(), true);
            BlockPos stand = DimensionStaffItem.jump(player, next);
            boolean solid = next.getBlockState(stand.below()).isSolid();
            boolean free = next.noCollision(player);
            bad += solid && free && player.level() == next ? 0 : 1;
            System.out.println("[DZSHOTS] staff " + next.dimension().location() + " stand=" + stand.toShortString()
                    + " below=" + BuiltInRegistries.BLOCK.getKey(next.getBlockState(stand.below()).getBlock()) + " solid=" + solid + " free=" + free);
        } while (next.dimension() != home);
        System.out.println("[DZSHOTS] staff bad=" + bad + " of " + server.levelKeys().size());
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (pendingShot != null && wait < 20) {
            String name = pendingShot;
            pendingShot = null;
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
            });
            System.out.println("[DZSHOTS] " + name);
        }
    }
}
