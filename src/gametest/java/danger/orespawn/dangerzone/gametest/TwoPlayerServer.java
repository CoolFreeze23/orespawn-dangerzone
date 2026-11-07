package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.BabySitter;
import danger.orespawn.dangerzone.entity.ChaosDemon;
import danger.orespawn.dangerzone.entity.Dargon;
import danger.orespawn.dangerzone.entity.DzBeast;
import danger.orespawn.dangerzone.entity.KrakenKing;
import danger.orespawn.dangerzone.entity.PrincessTeen;
import danger.orespawn.dangerzone.entity.TowerFlag;
import danger.orespawn.dangerzone.entity.Varmint;
import danger.orespawn.dangerzone.morph.Morph;
import danger.orespawn.dangerzone.world.DzBuilder;
import danger.orespawn.dangerzone.world.DzPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * The server half of the tests that need a real dedicated server. Logged as [DZ2] PASS, FAIL and NOTE lines. Set by
 * -Dorespawn_dangerzone.hubphase: twoplayer has two real clients, Alpha and Beta, playing by the server's normal rules and
 * reporting in chat what they saw; soak stands Alpha in each place in turn and counts what the game spawns; flag30 runs
 * the Tower Defense Flag at its real pace with nobody there.
 */
public final class TwoPlayerServer {
    private record Step(String name, BooleanSupplier run, int timeout) {
    }

    private static final String PHASE = System.getProperty("orespawn_dangerzone.hubphase", "twoplayer");
    private static final java.util.Map<String, Integer> JOINED = new java.util.TreeMap<>();
    private static boolean counting;
    private static final List<Step> STEPS = new ArrayList<>();
    private static final Set<String> SAID = new HashSet<>();
    private static final Set<String> GONE = new HashSet<>();
    private static int current;
    private static int stepTicks;
    private static int passes;
    private static int fails;
    private static boolean built;
    private static boolean finished;
    private static int after;
    private static int nextX = 2000;

    private TwoPlayerServer() {
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("DZ2");

    /** Through the logger, so that it lands in the server's log file. */
    private static void log(String text) {
        LOGGER.info("[DZ2] {}", text);
    }

    private static void ok(boolean condition, String what) {
        if (condition) {
            passes++;
        } else {
            fails++;
        }
        log((condition ? "PASS " : "FAIL ") + what);
    }

    private static void once(String name, Runnable run) {
        STEPS.add(new Step(name, () -> {
            run.run();
            return true;
        }, 1));
    }

    private static void until(String name, int timeout, BooleanSupplier done) {
        STEPS.add(new Step(name, done, timeout));
    }

    private static void pause(int ticks) {
        int[] left = {ticks};
        STEPS.add(new Step("pause", () -> --left[0] <= 0, ticks + 1));
    }

    private static MinecraftServer server;

    private static ServerPlayer player(String name) {
        return server.getPlayerList().getPlayerByName(name);
    }

    private static ServerPlayer alpha() {
        return player("Alpha");
    }

    private static ServerPlayer beta() {
        return player("Beta");
    }

    private static ServerLevel overworld() {
        return server.overworld();
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        SAID.add(event.getUsername() + ":" + event.getRawText());
        log("chat " + event.getUsername() + ": " + event.getRawText());
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        GONE.add(event.getEntity().getGameProfile().getName());
        if (!finished) {
            log("NOTE " + event.getEntity().getGameProfile().getName() + " left the server");
        }
    }

    /** Test players can't be let die: a death would stop the script. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !expectDeath) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        }
    }

    private static boolean expectDeath;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        server = event.getServer();
        if (finished) {
            if (++after > 60) {
                server.halt(false);
            }
            return;
        }
        if (PHASE.startsWith("flag")) {
            flagTick();
            return;
        }
        if (alpha() == null || (beta() == null && PHASE.equals("twoplayer"))) {
            if (built) {
                ok(false, "both players still connected at step " + STEPS.get(Math.min(current, STEPS.size() - 1)).name() + ", gone: " + GONE);
                finish();
            }
            return;
        }
        if (!built) {
            built = true;
            buildScript();
            log("players are on: script of " + STEPS.size() + " steps, phase " + PHASE);
        }
        if (current >= STEPS.size()) {
            finish();
            return;
        }
        Step step = STEPS.get(current);
        boolean done;
        try {
            done = step.run().getAsBoolean();
        } catch (Throwable e) {
            ok(false, "step '" + step.name() + "' threw " + e);
            for (StackTraceElement line : e.getStackTrace()) {
                if (line.getClassName().contains("orespawn")) {
                    log("    at " + line);
                }
            }
            done = true;
        }
        if (!done && ++stepTicks >= step.timeout()) {
            ok(false, "timed out: " + step.name());
            done = true;
        }
        if (done) {
            current++;
            stepTicks = 0;
        }
    }

    private static void finish() {
        finished = true;
        log(String.format("RESULT passes=%d fails=%d", passes, fails));
    }

    private static BlockPos arena() {
        nextX += 500;
        ServerLevel level = overworld();
        int y = level.getChunk(new BlockPos(nextX, 0, 0)).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nextX & 15, 0);
        BlockPos at = new BlockPos(nextX, y + 1, 0);
        alpha().teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        beta().teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180.0f, 0.0f);
        return at;
    }

    private static <T extends Entity> T spawn(EntityType<T> type, ServerLevel level, double x, double y, double z) {
        T entity = type.create(level);
        entity.moveTo(x, y, z, 0.0f, 0.0f);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
        }
        level.addFreshEntity(entity);
        return entity;
    }

    private static void clearAround(ServerPlayer player) {
        player.serverLevel().getEntities(player, player.getBoundingBox().inflate(160.0), e -> !(e instanceof Player)).forEach(Entity::discard);
    }

    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        // New arrivals only, whatever brought them: not ones loaded back in from disk.
        if (counting && !event.getLevel().isClientSide() && !event.loadedFromDisk() && event.getEntity() instanceof Mob) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            if (id.getNamespace().equals(danger.orespawn.dangerzone.OreSpawnDangerZone.MOD_ID)) {
                JOINED.merge(id.getPath(), 1, Integer::sum);
            }
        }
    }

    // ---- the flag at its real pace: a wave every 30 seconds, nothing cleared away, and nobody defending it

    private static final int DEFENDED = 400;
    private static TowerFlag realFlag;
    private static int flagTicks;
    private static int flagRung = -1;
    private static double flagWorst;
    private static double flagTotal;
    private static int flagMost;

    private static void flagTick() {
        ServerLevel level = overworld();
        if (realFlag == null) {
            if (++flagTicks < 100) {
                return;
            }
            flagTicks = 0;
            // The chunk has to be there before it can say where its ground is.
            level.getChunk(300 >> 4, 300 >> 4);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(300, 0, 300));
            // Nobody is here, so the chunks are held open as a player's would be.
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, true);
                }
            }
            server.setDifficulty(Difficulty.NORMAL, true);
            realFlag = spawn(ModContent.TOWER_FLAG.get(), level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            log(String.format("flag30: planted at %s, %d rungs at %d s", at.toShortString(), TowerFlag.ladder().size(), danger.orespawn.dangerzone.DzConfig.FLAG_WAVE_SECONDS.get()));
            return;
        }
        flagTicks++;
        // Stands in for a good defender: nothing in the wave lasts more than DEFENDED ticks.
        if (PHASE.equals("flagdef") && flagTicks % 20 == 0) {
            level.getEntitiesOfClass(Mob.class, realFlag.getBoundingBox().inflate(128.0), m -> m != realFlag && m.tickCount > DEFENDED).forEach(Entity::discard);
        }
        double ms = server.getCurrentSmoothedTickTime();
        flagWorst = Math.max(flagWorst, ms);
        flagTotal += ms;
        if (!realFlag.isAlive() || realFlag.finished()) {
            log(String.format("flag30: %s on rung %d after %d minutes; most mobs at once %d, average tick %.0f ms, worst %.0f ms", realFlag.finished() ? "SURVIVED" : "FELL", flagRung, flagTicks / 1200, flagMost, flagTotal / flagTicks, flagWorst));
            finished = true;
            return;
        }
        if (realFlag.rung() != flagRung) {
            flagRung = realFlag.rung();
            var mobs = level.getEntitiesOfClass(Mob.class, realFlag.getBoundingBox().inflate(128.0), m -> m != realFlag);
            flagMost = Math.max(flagMost, mobs.size());
            if (flagRung % 4 == 0) {
                java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
                mobs.forEach(m -> kinds.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath(), 1, Integer::sum));
                var biggest = kinds.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(3).toList();
                log(String.format("flag30 rung %d: health %.0f of %.0f, %d mobs near (most: %s), tick %.0f ms, worst %.0f ms", flagRung, realFlag.getHealth(), realFlag.getMaxHealth(), mobs.size(), biggest, ms, flagWorst));
            }
        }
    }

    // ---- what actually turns up, by the game's own spawning, over real time

    private static void soakScript() {
        once("set up", () -> {
            server.setDifficulty(Difficulty.NORMAL, true);
            server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, server);
            alpha().getAbilities().invulnerable = true;
            alpha().getAbilities().mayfly = true;
            alpha().getAbilities().flying = true;
            alpha().onUpdateAbilities();
        });
        for (String dimension : List.of("orespawn:chaos", "orespawn:mining", "orespawn:village", "orespawn_dangerzone:kraken_realm", "orespawn_dangerzone:dragons_lair", "orespawn_dangerzone:doom", "minecraft:overworld")) {
            soakAt(dimension, 4800);
        }
    }

    private static void soakAt(String dimension, int ticks) {
        double[] worst = {0.0};
        once("to " + dimension, () -> {
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(dimension)));
            nextX += 1200;
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(alpha(), level, nextX, level.getMaxBuildHeight() - 40, 0);
            alpha().getAbilities().flying = true;
            alpha().onUpdateAbilities();
            JOINED.clear();
            counting = true;
        });
        int[] left = {ticks};
        until("soak " + dimension, ticks + 2, () -> {
            worst[0] = Math.max(worst[0], server.getCurrentSmoothedTickTime());
            ServerPlayer p = alpha();
            // Stays by the ground: the game's spawning works around a player and despawns what's far from one.
            if (left[0] % 100 == 0) {
                p.teleportTo(p.getX(), p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ()) + 3.0, p.getZ());
            }
            return --left[0] <= 0;
        });
        once("count " + dimension, () -> {
            counting = false;
            ServerPlayer p = alpha();
            java.util.Map<String, Integer> near = new java.util.TreeMap<>();
            p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(128.0, 400.0, 128.0)).forEach(m -> near.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath(), 1, Integer::sum));
            note(String.format("soak %s, %d minutes, worst tick %.0f ms", dimension, ticks / 1200, worst[0]));
            note("  add-on arrivals: " + JOINED);
            note("  around now: " + near);
            clearAround(p);
        });
    }

    private static void buildScript() {
        if (PHASE.equals("soak")) {
            soakScript();
            return;
        }
        once("rules", () -> {
            ok(!server.isFlightAllowed(), "this server has flight off, the default");
            ok(server.isDedicatedServer(), "and is a real dedicated server");
            server.setDifficulty(Difficulty.NORMAL, true);
            server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            overworld().setDayTime(6000L);
        });
        morphAcrossClients();
        theWarpTakesOnlyTheKiller();
        ownership();
        sharedFlag();
        ridingDargon();
        wornOutWelcome();
    }

    // ---- what one player does, the other sees

    private static void morphAcrossClients() {
        once("morph set up", () -> arena());
        pause(60);
        once("alpha morphs", () -> {
            SAID.clear();
            Morph.set(alpha(), "orespawn_dangerzone:hyena");
        });
        until("beta sees it", 200, () -> SAID.contains("Beta:saw Alpha as orespawn_dangerzone:hyena"));
        once("beta result", () -> ok(SAID.contains("Beta:saw Alpha as orespawn_dangerzone:hyena"), "Beta's client saw Alpha turn into a Hyena"));
        once("beta leaves view", () -> {
            SAID.clear();
            beta().teleportTo(overworld(), beta().getX() + 3000.0, 150.0, beta().getZ(), 0.0f, 0.0f);
            Morph.set(alpha(), "orespawn_dangerzone:warthog");
        });
        pause(80);
        once("beta comes back", () -> {
            ok(!SAID.contains("Beta:saw Alpha as orespawn_dangerzone:warthog"), "Beta, out of sight, wasn't told about the change");
            beta().teleportTo(overworld(), alpha().getX(), alpha().getY(), alpha().getZ() + 4.0, 180.0f, 0.0f);
        });
        until("beta sees the change", 300, () -> SAID.contains("Beta:saw Alpha as orespawn_dangerzone:warthog"));
        once("result", () -> ok(SAID.contains("Beta:saw Alpha as orespawn_dangerzone:warthog"), "coming back into view, Beta saw Alpha's new form"));
        once("out of sight, back to normal", () -> {
            SAID.clear();
            beta().teleportTo(overworld(), beta().getX() + 3000.0, 150.0, beta().getZ(), 0.0f, 0.0f);
            Morph.set(alpha(), "");
        });
        pause(80);
        once("beta returns to a plain Alpha", () -> beta().teleportTo(overworld(), alpha().getX(), alpha().getY(), alpha().getZ() + 4.0, 180.0f, 0.0f));
        until("beta sees the plain Alpha", 300, () -> SAID.contains("Beta:saw Alpha as themselves"));
        once("plain result", () -> {
            ok(SAID.contains("Beta:saw Alpha as themselves"), "Alpha went back out of Beta's sight, and Beta saw the player on return");
            SAID.clear();
            Morph.set(alpha(), "orespawn_dangerzone:warthog");
        });
        until("beta sees it again", 200, () -> SAID.contains("Beta:saw Alpha as orespawn_dangerzone:warthog"));
        once("really dies", () -> {
            SAID.clear();
            expectDeath = true;
            alpha().hurt(alpha().damageSources().genericKill(), Float.MAX_VALUE);
            expectDeath = false;
        });
        // The client answers the death screen itself, as a person does.
        until("alpha respawns", 300, () -> alpha().isAlive());
        pause(40);
        // A respawn is at world spawn, far out of Beta's sight: Beta can only find out on meeting them.
        once("beta finds alpha", () -> beta().teleportTo(overworld(), alpha().getX(), alpha().getY(), alpha().getZ() + 4.0, 180.0f, 0.0f));
        until("beta told", 200, () -> SAID.contains("Beta:saw Alpha as themselves"));
        once("death result", () -> {
            ok(Morph.form(alpha()).isEmpty(), "dying ended Alpha's form");
            ok(SAID.contains("Beta:saw Alpha as themselves"), "and Beta was told");
        });
    }

    // ---- the warp, with a bystander standing closer

    private static void theWarpTakesOnlyTheKiller() {
        BabySitter[] sitter = new BabySitter[1];
        once("warp set up", () -> {
            BlockPos at = arena();
            sitter[0] = spawn(ModContent.BABY_SITTER.get(), overworld(), at.getX() + 0.5, at.getY(), at.getZ() + 8.5);
            // Beta stands right beside her, Alpha well back: the old warp took whoever was nearest.
            beta().teleportTo(overworld(), at.getX() + 3.5, at.getY(), at.getZ() + 8.5, 0.0f, 0.0f);
            alpha().teleportTo(overworld(), at.getX() + 0.5, at.getY(), at.getZ() - 12.5, 0.0f, 0.0f);
        });
        pause(40);
        once("alpha kills her", () -> {
            sitter[0].hurt(overworld().damageSources().playerAttack(alpha()), Float.MAX_VALUE);
            ok(!sitter[0].isAlive(), "Alpha killed the Baby Sitter");
        });
        until("alpha warped", 300, () -> alpha().level().dimension() == DzPlaces.KRAKEN_REALM);
        once("warp result", () -> {
            ok(alpha().level().dimension() == DzPlaces.KRAKEN_REALM, "Alpha, the killer, was warped though further away");
            ok(beta().level().dimension() == Level.OVERWORLD, "Beta, the bystander, stayed where they were");
        });
        until("castle up", 1200, () -> DzBuilder.activeCount() == 0);
        once("come home", () -> {
            clearAround(alpha());
            alpha().teleportTo(overworld(), beta().getX(), beta().getY(), beta().getZ() + 4.0, 0.0f, 0.0f);
            ok(alpha().level().dimension() == Level.OVERWORLD, "and could be brought back");
        });
        pause(60);
    }

    // ---- what's yours stays yours

    private static void ownership() {
        PrincessTeen[] teen = new PrincessTeen[1];
        Varmint[] pet = new Varmint[1];
        once("pets set up", () -> {
            clearAround(alpha());
            BlockPos at = arena();
            teen[0] = spawn(ModContent.PRINCESS_TEEN.get(), overworld(), at.getX() + 4.5, at.getY(), at.getZ() + 2.5);
            teen[0].tame(alpha());
            pet[0] = spawn(ModContent.VARMINT.get(), overworld(), at.getX() - 4.5, at.getY() + 2.0, at.getZ() + 2.5);
            pet[0].tame(alpha());
        });
        pause(60);
        once("beta tries", () -> {
            beta().teleportTo(overworld(), teen[0].getX() + 1.0, teen[0].getY(), teen[0].getZ(), 0.0f, 0.0f);
            beta().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_BLOCK));
            beta().interactOn(teen[0], InteractionHand.MAIN_HAND);
            ok(teen[0].isOwnedBy(alpha()) && beta().getMainHandItem().getCount() == 1, "Beta's diamond block didn't take Alpha's Teen Princess");
            beta().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF));
            beta().interactOn(pet[0], InteractionHand.MAIN_HAND);
            ok(pet[0].isOwnedBy(alpha()) && beta().getMainHandItem().getCount() == 1, "nor did Beta's meat take Alpha's Varmint");
        });
        once("a pet's enemy", () -> {
            pet[0].setTarget(null);
            beta().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            alpha().hurt(alpha().damageSources().playerAttack(beta()), 0.5f);
        });
        pause(5);
        once("away", () -> {
            alpha().teleportTo(overworld(), alpha().getX() + 80.0, alpha().getY(), alpha().getZ(), 0.0f, 0.0f);
        });
        pause(300);
        once("the Varmint follows", () -> {
            note("Varmint is " + pet[0].distanceTo(alpha()) + " from its owner");
            ok(pet[0].isAlive() && pet[0].distanceTo(alpha()) < 30.0, "the Varmint caught up with its owner");
            clearAround(alpha());
            clearAround(beta());
        });
    }

    private static void note(String text) {
        log("NOTE " + text);
    }

    // ---- two people on one flag

    private static void sharedFlag() {
        TowerFlag[] flag = new TowerFlag[1];
        once("flag set up", () -> arena());
        pause(60);
        once("alpha plants it", () -> {
            ServerPlayer a = alpha();
            ItemStack stack = new ItemStack(ModContent.TOWER_FLAG_ITEM.get());
            a.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos under = a.blockPosition().below().east(6);
            a.gameMode.useItemOn(a, a.serverLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(under).add(0.0, 0.5, 0.0), Direction.UP, under, false));
            flag[0] = a.serverLevel().getEntitiesOfClass(TowerFlag.class, a.getBoundingBox().inflate(16.0)).stream().findFirst().orElse(null);
            ok(flag[0] != null, "Alpha planted a flag");
            SAID.clear();
        });
        once("waves", () -> {
            for (int i = 0; i < 6; i++) {
                flag[0].nextWave(overworld());
            }
        });
        pause(100);
        once("flag result", () -> {
            ok(flag[0].isAlive(), "it stood up to a few waves with both players there");
            flag[0].discard();
            clearAround(alpha());
        });
    }

    // ---- the flight check: Dargon's rider, with allow-flight off

    private static void ridingDargon() {
        Dargon[] dargon = new Dargon[1];
        Vec3[] from = new Vec3[1];
        int[] held = new int[1];
        once("dargon set up", () -> arena());
        pause(60);
        once("tame him", () -> {
            ServerPlayer a = alpha();
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            dargon[0] = spawn(ModContent.DARGON.get(), overworld(), a.getX() + 6.0, a.getY(), a.getZ());
            dargon[0].setHealth(dargon[0].getMaxHealth() * 0.2f);
            a.interactOn(dargon[0], InteractionHand.MAIN_HAND);
            ok(dargon[0].isTame() && a.getVehicle() == dargon[0], "Alpha tamed Dargon and is riding");
            from[0] = dargon[0].position();
            SAID.clear();
        });
        // 10 seconds in the air: past the 4 seconds that gets a floating vehicle's rider kicked.
        until("ride", 300, () -> {
            held[0]++;
            return held[0] >= 280 || alpha() == null;
        });
        once("ride result", () -> {
            ok(alpha() != null, "Alpha wasn't kicked for flying after " + held[0] / 20 + "s in the air with flight off");
            double moved = dargon[0].position().distanceTo(from[0]);
            note(String.format("Dargon flew %.1f blocks, %.1f high", moved, dargon[0].getY() - from[0].y));
            ok(moved > 10.0, "the rider's client flew him and the server accepted it");
            ok(alpha().getVehicle() == dargon[0], "still riding");
            alpha().stopRiding();
            dargon[0].discard();
        });
        pause(20);
    }

    // ---- leaving and coming back

    private static void wornOutWelcome() {
        once("the last word", () -> {
            SAID.clear();
            log("asking the clients to confirm they're fine");
            server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal("report"), false);
        });
        until("alpha ok", 100, () -> SAID.contains("Alpha:all fine"));
        until("beta ok", 100, () -> SAID.contains("Beta:all fine"));
        once("reports", () -> ok(SAID.contains("Alpha:all fine") && SAID.contains("Beta:all fine"), "both clients were still running and connected at the end"));
    }
}
