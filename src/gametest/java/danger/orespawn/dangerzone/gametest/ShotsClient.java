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
    /** A short run for chasing one problem: no creature photos, and only the Kraken realm. */
    private static final boolean FAST = Boolean.getBoolean("orespawn_dangerzone.fast");
    private static int krakensSeen;

    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getType() == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("orespawn:kraken"))) {
            int n = ++krakensSeen;
            if (n <= 12 || n % 40 == 0) {
                StringBuilder trace = new StringBuilder();
                for (StackTraceElement e : new Throwable().getStackTrace()) {
                    String c = e.getClassName();
                    if (c.contains("orespawn") || c.contains("NaturalSpawner") || c.contains("DzBuilder") || c.contains("Spawner")) {
                        trace.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber()).append(' ');
                    }
                }
                System.out.println("[DZSHOTS] kraken#" + n + " in " + event.getLevel().dimension().location() + " loaded=" + event.loadedFromDisk() + " " + trace);
            }
        }
    }

    private static int placeStep;
    private static final int PLACE_STEPS = 13;
    private static final int[] PLACE_WAITS = {120, 300, 60, 100, 120, 500, 60, 100, 60, 100, 200, 200, 60};
    private static String framedName;
    private static net.minecraft.core.BlockPos torch;
    private static danger.orespawn.dangerzone.entity.DoomWorm worm;
    private static net.minecraft.world.phys.Vec3 wormStart;
    private static long stepStart;
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
                SUBJECTS.add(new Subject("chaos_demon", ModContent.CHAOS_DEMON.get(), 7.0, 1.5));
                SUBJECTS.add(new Subject("baby_sitter", ModContent.BABY_SITTER.get(), 20.0, 7.0));
                SUBJECTS.add(new Subject("kraken_king", ModContent.KRAKEN_KING.get(), 30.0, 20.0));
                SUBJECTS.add(new Subject("squid_guard", ModContent.SQUID_GUARD.get(), 6.0, 1.0));
                SUBJECTS.add(new Subject("dargon", ModContent.DARGON.get(), 50.0, 10.0));
                SUBJECTS.add(new Subject("element_dragon", ModContent.ELEMENT_DRAGON.get(), 12.0, 2.0));
                SUBJECTS.add(new Subject("doom_worm", ModContent.DOOM_WORM.get(), 40.0, 6.0));
                SUBJECTS.add(new Subject("drant", ModContent.DRANT.get(), 2.5, 0.2));
                SUBJECTS.add(new Subject("doom_ant", ModContent.DOOM_ANT.get(), 2.5, 0.2));
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
                if (FAST) {
                    SUBJECTS.clear();
                    staffChecked = true;
                    liveStep = LIVE_STEPS;
                    placeStep = 0;
                }
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
            if (placeStep < PLACE_STEPS) {
                final int step = placeStep++;
                mc.getSingleplayerServer().execute(() -> places(step));
                wait = PLACE_WAITS[step];
                return;
            }
            if (framedName != null) {
                pendingShot = framedName;
                framedName = null;
                wait = 40;
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
                // The King's tentacles hang well below him, so he's shown as he flies.
                current.moveTo(0.5 + subject * 100.0, s.name().equals("kraken_king") ? -38.0 : -60.0, 0.5, 0.0f, 0.0f);
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

    /** The three new places, for real: the warp and its castle, the ritual and its carve, Doom and its worm. */
    private static void places(int step) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = true;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        ServerLevel realm = server.getLevel(danger.orespawn.dangerzone.world.DzPlaces.KRAKEN_REALM);
        ServerLevel lair = server.getLevel(danger.orespawn.dangerzone.world.DzPlaces.DRAGONS_LAIR);
        ServerLevel doom = server.getLevel(danger.orespawn.dangerzone.world.DzPlaces.DOOM);
        double tick = server.getCurrentSmoothedTickTime();
        server.overworld().setDayTime(6000L);
        server.overworld().setWeatherParameters(6000, 0, false, false);
        switch (step) {
            case 0 -> {
                System.out.println("[DZSHOTS] place dims realm=" + (realm != null) + " lair=" + (lair != null) + " doom=" + (doom != null));
                danger.orespawn.dangerzone.world.DzPlaces.warpToRealm(player);
                System.out.println("[DZSHOTS] place warped to " + player.level().dimension().location() + " at " + player.blockPosition().toShortString()
                        + " builders=" + danger.orespawn.dangerzone.world.DzBuilder.activeCount());
            }
            case 1 -> {
                var castle = danger.orespawn.dangerzone.world.DzPlaces.nearestCastle(player.getX(), player.getZ());
                var box = new net.minecraft.world.phys.AABB(castle.getMinBlockX() - 32, 40, castle.getMinBlockZ() - 32, castle.getMinBlockX() + 96, 120, castle.getMinBlockZ() + 96);
                System.out.println("[DZSHOTS] place castle builders=" + danger.orespawn.dangerzone.world.DzBuilder.activeCount()
                        + " krakensSeen=" + krakensSeen
                        + " guards=" + realm.getEntitiesOfClass(danger.orespawn.dangerzone.entity.SquidGuard.class, box).size()
                        + " demons=" + realm.getEntitiesOfClass(danger.orespawn.dangerzone.entity.ChaosDemon.class, box).size()
                        + " sitters=" + realm.getEntitiesOfClass(danger.orespawn.dangerzone.entity.BabySitter.class, box).size()
                        + " tickMs=" + String.format("%.1f", tick));
                int x = castle.getMinBlockX() + 26;
                int z = castle.getMinBlockZ() + 21;
                int chests = 0;
                for (int dx = -2; dx < 60; dx++) {
                    for (int dz = -2; dz < 50; dz++) {
                        for (int y = 25; y < 100; y++) {
                            if (realm.getBlockState(new net.minecraft.core.BlockPos(castle.getMinBlockX() + dx, y, castle.getMinBlockZ() + dz)).is(net.minecraft.world.level.block.Blocks.CHEST)) {
                                chests++;
                            }
                        }
                    }
                }
                System.out.println("[DZSHOTS] place castle chests=" + chests);
                player.teleportTo(realm, x - 60.0, 110.0, z - 60.0, -45.0f, 25.0f);
                framedName = "dz_place_castle.png";
                realm.setDayTime(6000L);
            }
            case 2 -> {
                if (framedName != null) {
                    pendingShot = framedName;
                    framedName = null;
                }
                var castle = danger.orespawn.dangerzone.world.DzPlaces.nearestCastle(player.getX(), player.getZ());
                net.minecraft.world.entity.Mob king = ModContent.KRAKEN_KING.get().create(realm);
                king.moveTo(player.getX(), player.getY() - 6.0, player.getZ() + 20.0, 0.0f, 0.0f);
                king.setNoAi(true);
                realm.addFreshEntity(king);
                king.hurt(realm.damageSources().playerAttack(player), 10.0f);
                System.out.println("[DZSHOTS] place king health=" + king.getHealth() + " princessNear=" + realm.getEntitiesOfClass(danger.orespawn.dangerzone.entity.PrincessYoungAdult.class, king.getBoundingBox().inflate(32.0)).size());
            }
            case 3 -> {
                if (FAST) {
                    var mobs = realm.getBiome(player.blockPosition()).value().getMobSettings();
                    for (var category : net.minecraft.world.entity.MobCategory.values()) {
                        System.out.println("[DZSHOTS] realm " + category + " " + mobs.getMobs(category).unwrap().stream().map(d -> BuiltInRegistries.ENTITY_TYPE.getKey(d.type) + "x" + d.getWeight().asInt()).toList());
                    }
                    System.out.println("[DZSHOTS] krakens total seen " + krakensSeen);
                    placeStep = PLACE_STEPS;
                    return;
                }
                danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player, lair, 0, 140, 0);
            }
            case 4 -> {
                int y = lair.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                var ground = new net.minecraft.core.BlockPos(0, y, 0);
                System.out.println("[DZSHOTS] place lair surface y=" + y + " below=" + BuiltInRegistries.BLOCK.getKey(lair.getBlockState(ground.below()).getBlock()));
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        lair.setBlockAndUpdate(ground.offset(dx, -1, dz), ModContent.DRAGONS_EYE_STONE.get().defaultBlockState());
                    }
                }
                torch = ground;
                player.teleportTo(lair, 0.5, y + 1.0, -2.5, 0.0f, 0.0f);
                lair.setBlockAndUpdate(ground, ModContent.EXTREME_TORCH.get().defaultBlockState());
                stepStart = System.currentTimeMillis();
                System.out.println("[DZSHOTS] place ritual lit");
            }
            case 5, 6 -> System.out.println("[DZSHOTS] place carve builders=" + danger.orespawn.dangerzone.world.DzBuilder.activeCount() + " seconds=" + (System.currentTimeMillis() - stepStart) / 1000
                    + " tickMs=" + String.format("%.1f", tick) + " platform=" + BuiltInRegistries.BLOCK.getKey(lair.getBlockState(torch).getBlock()) + " playerY=" + player.getBlockY());
            case 7 -> {
                var box = new net.minecraft.world.phys.AABB(torch).inflate(120.0, 60.0, 260.0);
                System.out.println("[DZSHOTS] place lair done builders=" + danger.orespawn.dangerzone.world.DzBuilder.activeCount()
                        + " seconds=" + (System.currentTimeMillis() - stepStart) / 1000
                        + " dargon=" + lair.getEntitiesOfClass(danger.orespawn.dangerzone.entity.Dargon.class, box).size()
                        + " elements=" + lair.getEntitiesOfClass(danger.orespawn.dangerzone.entity.ElementDragon.class, box).size()
                        + " airAtMidCarve=" + lair.getBlockState(torch.offset(30, 20, 50)).isAir()
                        + " bridge=" + BuiltInRegistries.BLOCK.getKey(lair.getBlockState(torch.offset(0, 0, 50)).getBlock()));
                player.teleportTo(lair, torch.getX() + 0.5, torch.getY() + 14.0, torch.getZ() + 40.5, 0.0f, 8.0f);
                framedName = "dz_place_lair.png";
            }
            case 8 -> {
                if (framedName != null) {
                    pendingShot = framedName;
                    framedName = null;
                }
            }
            case 9 -> danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player, doom, 0, 60, 0);
            case 10 -> {
                worm = ModContent.DOOM_WORM.get().create(doom);
                worm.moveTo(40.5, 45.0, 0.5, 0.0f, 0.0f);
                doom.addFreshEntity(worm);
                wormStart = worm.position();
                player.teleportTo(doom, 0.5, 58.0, 0.5, -90.0f, 20.0f);
                System.out.println("[DZSHOTS] place worm placed; surface=" + doom.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 40, 0));
            }
            case 11 -> {
                System.out.println("[DZSHOTS] place worm moved=" + String.format("%.1f", worm.position().distanceTo(wormStart)) + " distToPlayer=" + String.format("%.1f", worm.distanceTo(player))
                        + " y=" + String.format("%.1f", worm.getY()) + " holeAtStart=" + doom.getBlockState(net.minecraft.core.BlockPos.containing(wormStart)).isAir()
                        + " tickMs=" + String.format("%.1f", tick));
                framedName = "dz_place_doom.png";
                player.teleportTo(doom, worm.getX() - 30.0, worm.getY() + 18.0, worm.getZ() - 30.0, -45.0f, 25.0f);
            }
            default -> {
                if (framedName != null) {
                    pendingShot = framedName;
                    framedName = null;
                }
                long ants = doom.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, player.getBoundingBox().inflate(128.0, 256.0, 128.0)).size();
                System.out.println("[DZSHOTS] place sand falling=" + ants + " wormAlive=" + worm.isAlive());
                worm.discard();
            }
        }
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
