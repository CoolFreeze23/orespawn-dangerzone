package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BabySitter;
import danger.orespawn.dangerzone.entity.ChaosDemon;
import danger.orespawn.dangerzone.entity.Dargon;
import danger.orespawn.dangerzone.entity.DelayedEvent;
import danger.orespawn.dangerzone.entity.DoomWorm;
import danger.orespawn.dangerzone.entity.ElementDragon;
import danger.orespawn.dangerzone.entity.ElementShot;
import danger.orespawn.dangerzone.entity.KrakenKing;
import danger.orespawn.dangerzone.entity.Martian;
import danger.orespawn.dangerzone.entity.PrincessTeen;
import danger.orespawn.dangerzone.entity.PrincessYoungAdult;
import danger.orespawn.dangerzone.entity.ShrinkBolt;
import danger.orespawn.dangerzone.entity.Swarms;
import danger.orespawn.dangerzone.entity.TowerFlag;
import danger.orespawn.dangerzone.entity.Trophy;
import danger.orespawn.dangerzone.item.TrophyItem;
import danger.orespawn.dangerzone.world.DzBuilder;
import danger.orespawn.dangerzone.world.DzPlaces;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

/**
 * Plays the mod in a real world, in a hidden window, driving a real player through the game's own actions, and logs
 * what happened as [DZT] lines: PASS, FAIL and measurements. Run with -Pdzdrive and, for the later stages, -Pdzphase.
 *
 * Phases: all (everything in a fresh world, then a quit and reload of it); interrupt (light the ritual and quit part way);
 * resume (open that world and see it finish); oldworld (open the oldest world on disk, made before these dimensions).
 */
public final class TestDrive {
    private record Step(String name, BooleanSupplier run, int timeout) {
    }

    private static final String PHASE = System.getProperty("orespawn_dangerzone.phase", "all");
    private static final List<Step> STEPS = new ArrayList<>();
    private static int current;
    private static int stepTicks;
    private static boolean started;
    private static boolean built;
    private static String worldName;
    private static int passes;
    private static int fails;
    private static boolean reloadRequested;
    private static boolean reloading;
    private static boolean stopAtEnd = true;
    private static int reopenWait;
    private static volatile long lastServerTick;
    private static volatile boolean holdKeys;
    private static volatile float clientForward;
    private static volatile boolean clientRiding;
    private static final java.util.Set<String> TRACE = java.util.Set.of("bloodlust_ant", "baby_sitter", "dargon");
    private static final long HUNG_AFTER_MS = 90_000L;

    // what the player has taken, for the fight records
    private static int hits;
    private static float totalDamage;
    private static float biggestHit;
    private static int firstHitTick = -1;
    private static int fightTicks;
    private static boolean fightOn;

    private static final double ARENA_Y = -60.0;
    private static int nextArena;

    private TestDrive() {
    }

    // ---------------------------------------------------------------- reporting

    private static void log(String text) {
        System.out.println("[DZT] " + text);
    }

    private static void ok(boolean condition, String what) {
        if (condition) {
            passes++;
            log("PASS " + what);
        } else {
            fails++;
            log("FAIL " + what);
        }
    }

    private static void note(String what) {
        log("NOTE " + what);
    }

    // ---------------------------------------------------------------- script building

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

    // ---------------------------------------------------------------- the world and the player

    private static MinecraftServer server() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    private static ServerPlayer player() {
        return server().getPlayerList().getPlayers().get(0);
    }

    private static ServerLevel overworld() {
        return server().overworld();
    }

    private static ServerLevel level(ResourceKey<Level> key) {
        return server().getLevel(key);
    }

    private static void survival() {
        ServerPlayer p = player();
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().flying = false;
        p.getAbilities().mayfly = false;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
    }

    private static void hold(Item item) {
        player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
    }

    private static void holdNothing() {
        player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /** A clean patch of the flat overworld, well away from the last one. */
    private static BlockPos newArena() {
        nextArena += 400;
        ServerLevel level = overworld();
        ServerPlayer p = player();
        BlockPos at = BlockPos.containing(nextArena + 0.5, ARENA_Y, 0.5);
        level.getChunk(at);
        p.teleportTo(level, at.getX() + 0.5, ARENA_Y, at.getZ() + 0.5, 0.0f, 0.0f);
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

    /** Clears everything but the player from around them. */
    private static void clearAround() {
        ServerPlayer p = player();
        p.serverLevel().getEntities(p, p.getBoundingBox().inflate(160.0), e -> !(e instanceof Player)).forEach(Entity::discard);
    }

    private static void attack(LivingEntity target) {
        ServerPlayer p = player();
        p.teleportTo(p.serverLevel(), target.getX() + target.getBbWidth() / 2.0 + 1.0, target.getY(), target.getZ(), 90.0f, 0.0f);
        p.resetAttackStrengthTicker();
        p.getAttributes().getInstance(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(1024.0);
        p.attack(target);
    }

    private static <T extends Entity> T nearest(Class<T> type, double radius) {
        ServerPlayer p = player();
        return p.serverLevel().getEntitiesOfClass(type, p.getBoundingBox().inflate(radius)).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    private static boolean has(Item item) {
        return player().getInventory().contains(new ItemStack(item));
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        if (fightOn && event.getEntity() instanceof ServerPlayer) {
            hits++;
            totalDamage += event.getNewDamage();
            biggestHit = Math.max(biggestHit, event.getNewDamage());
            if (firstHitTick < 0) {
                firstHitTick = fightTicks;
            }
        }
    }

    /** The test player can't be allowed to die: it'd stop being at the end of the script. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            hits++;
        }
    }

    // ---------------------------------------------------------------- the driver

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.getOverlay() == null && mc.screen != null) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                openStartingWorld(mc);
            }
            return;
        }
        acceptPrompts(mc);
        if (built && !reloading && !reloadRequested && lastServerTick != 0L && System.currentTimeMillis() - lastServerTick > HUNG_AFTER_MS) {
            fails++;
            log("FAIL the server hasn't ticked for " + HUNG_AFTER_MS / 1000 + "s, at step " + current);
            Thread.getAllStackTraces().forEach((thread, stack) -> {
                if (thread.getName().equals("Server thread")) {
                    for (StackTraceElement line : stack) {
                        log("    " + line);
                    }
                }
            });
            log(String.format("RESULT passes=%d fails=%d (hung)", passes, fails));
            Runtime.getRuntime().halt(3);
        }
        if (reloading) {
            if (mc.level == null && mc.screen instanceof TitleScreen && --reopenWait <= 0) {
                reloading = false;
                lastServerTick = 0L;
                log("reopening " + worldName);
                mc.createWorldOpenFlows().openWorld(worldName, () -> {
                    fails++;
                    log("FAIL the world wouldn't reopen");
                    stopAtEnd = true;
                    mc.stop();
                });
            }
            return;
        }
        if (reloadRequested) {
            reloadRequested = false;
            reloading = true;
            reopenWait = 100;
            log("quitting to the title screen");
            if (mc.level != null) {
                mc.level.disconnect();
            }
            mc.disconnect(new GenericMessageScreen(Component.literal("Saving world")));
            mc.setScreen(new TitleScreen());
            return;
        }
        if (current >= STEPS.size() && built) {
            log(String.format("RESULT passes=%d fails=%d", passes, fails));
            log("done");
            mc.stop();
        }
    }

    /** The warning any world with data-driven dimensions gets when it's opened: the port's own dimensions bring it too. */
    private static void acceptPrompts(Minecraft mc) {
        if (mc.screen == null || mc.screen instanceof TitleScreen) {
            return;
        }
        for (var child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.Button button
                    && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                    && key.getKey().equals("selectWorld.backupJoinSkipButton")) {
                log("answering the world-open warning: " + mc.screen.getClass().getSimpleName());
                button.onPress();
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (holdKeys) {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
        }
        if (mc.player != null) {
            clientForward = mc.player.zza;
            clientRiding = mc.player.getVehicle() != null && mc.player.getVehicle().isControlledByLocalInstance();
        }
    }

    private static void openStartingWorld(Minecraft mc) {
        if (PHASE.equals("all") || PHASE.equals("interrupt") || PHASE.equals("focus") || PHASE.equals("dargon")) {
            worldName = "drive" + PHASE;
            deleteWorldNamed(mc, worldName);
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            mc.createWorldOpenFlows().createFreshLevel(worldName,
                    new LevelSettings(worldName, GameType.SURVIVAL, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return;
        }
        worldName = PHASE.equals("resume") ? "driveinterrupt" : oldestWorld(mc);
        log("opening " + worldName);
        mc.createWorldOpenFlows().openWorld(worldName, () -> {
            fails++;
            log("FAIL the world wouldn't open");
            mc.stop();
        });
    }

    private static void deleteWorldNamed(Minecraft mc, String name) {
        File dir = new File(mc.gameDirectory, "saves/" + name);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static String oldestWorld(Minecraft mc) {
        File[] saves = new File(mc.gameDirectory, "saves").listFiles(f -> f.isDirectory() && f.getName().startsWith("shots"));
        Arrays.sort(saves, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        return saves[0].getName();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        lastServerTick = System.currentTimeMillis();
        if (event.getServer().getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        if (!built) {
            built = true;
            buildScript();
            log("script of " + STEPS.size() + " steps, phase " + PHASE);
        }
        if (fightOn) {
            fightTicks++;
        }
        // Keeps the player alive and fed through everything thrown at them.
        ServerPlayer p = player();
        if (p.getHealth() < p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
        if (current >= STEPS.size() || reloadRequested || reloading) {
            return;
        }
        Step step = STEPS.get(current);
        boolean done;
        try {
            done = step.run().getAsBoolean();
        } catch (Throwable e) {
            fails++;
            log("FAIL step '" + step.name() + "' threw " + e);
            for (StackTraceElement line : e.getStackTrace()) {
                if (line.getClassName().contains("orespawn")) {
                    log("    at " + line);
                }
            }
            done = true;
        }
        if (!done && ++stepTicks >= step.timeout()) {
            fails++;
            log("FAIL timed out: " + step.name() + " after " + stepTicks + " ticks");
            done = true;
        }
        if (done) {
            current++;
            stepTicks = 0;
        }
    }

    // ---------------------------------------------------------------- the script

    private static void buildScript() {
        switch (PHASE) {
            case "interrupt" -> interruptScript();
            case "resume" -> resumeScript();
            case "oldworld" -> oldWorldScript();
            case "focus" -> focusScript();
            case "dargon" -> {
                once("set up", () -> {
                    survival();
                    overworld().setDayTime(6000L);
                });
                for (int i = 0; i < 5; i++) {
                    fight("dargon", ModContent.DARGON.get(), 40, 600, true);
                }
            }
            default -> {
                once("set up", () -> {
                    survival();
                    overworld().setDayTime(6000L);
                    server().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, server());
                });
                assetChecks();
                dataChecks();
                itemChecks();
                everyCreatureFights();
                princessStages();
                nurseBug();
                trophiesOfEverything();
                flagLadder();
                krakenChain();
                walkToACastle();
                lairPlaythrough();
                doomPlaythrough();
                portalAnts();
                advancementsFire();
                naturalSpawning();
                saveAndReload();
            }
        }
    }

    // ---- assets: every item, block, entity, language key and sound resolves

    private static void assetChecks() {
        once("assets", () -> {
            Minecraft mc = Minecraft.getInstance();
            var missingModel = mc.getModelManager().getMissingModel();
            List<String> noModel = new ArrayList<>();
            List<String> noName = new ArrayList<>();
            int items = 0;
            for (var holder : ModContent.ITEMS.getEntries()) {
                Item item = holder.get();
                items++;
                if (mc.getItemRenderer().getItemModelShaper().getItemModel(item) == missingModel) {
                    noModel.add(holder.getId().getPath());
                }
                if (item != ModContent.TROPHY.get() && !Language.getInstance().has(item.getDescriptionId())) {
                    noName.add(holder.getId().getPath());
                }
            }
            ok(noModel.isEmpty(), "all " + items + " items have models; missing: " + noModel);
            ok(noName.isEmpty(), "all items are named; missing: " + noName);
            List<String> blockBad = new ArrayList<>();
            for (var holder : ModContent.BLOCKS.getEntries()) {
                if (mc.getBlockRenderer().getBlockModel(holder.get().defaultBlockState()) == missingModel) {
                    blockBad.add("model:" + holder.getId().getPath());
                }
                if (!Language.getInstance().has(holder.get().getDescriptionId())) {
                    blockBad.add("name:" + holder.getId().getPath());
                }
            }
            ok(blockBad.isEmpty(), "all " + ModContent.BLOCKS.getEntries().size() + " blocks have models and names; bad: " + blockBad);
            List<String> entityBad = new ArrayList<>();
            for (var holder : ModContent.ENTITIES.getEntries()) {
                if (!Language.getInstance().has(holder.get().getDescriptionId())) {
                    entityBad.add(holder.getId().getPath());
                }
            }
            ok(entityBad.isEmpty(), "all " + ModContent.ENTITIES.getEntries().size() + " entities are named; missing: " + entityBad);
            List<String> soundBad = new ArrayList<>();
            for (var holder : ModContent.SOUNDS.getEntries()) {
                var sounds = mc.getSoundManager().getSoundEvent(holder.getId());
                if (sounds == null) {
                    soundBad.add(holder.getId().getPath());
                }
            }
            ok(soundBad.isEmpty(), "all " + ModContent.SOUNDS.getEntries().size() + " sound events resolve; missing: " + soundBad);
        });
    }

    // ---- data: every loot table rolls, every recipe works

    private static void dataChecks() {
        once("loot", () -> {
            ServerLevel level = overworld();
            List<String> empty = new ArrayList<>();
            int rolled = 0;
            for (var holder : ModContent.ENTITIES.getEntries()) {
                EntityType<?> type = holder.get();
                Entity made = type.create(level);
                if (!(made instanceof LivingEntity living) || holder.getId().getPath().equals("volcano")) {
                    continue;
                }
                LootParams params = new LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, living)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                        .create(LootContextParamSets.ENTITY);
                LootTable table = server().reloadableRegistries().getLootTable(living.getLootTable());
                boolean any = false;
                for (int i = 0; i < 20 && !any; i++) {
                    any = !table.getRandomItems(params).isEmpty();
                }
                rolled++;
                if (!any) {
                    empty.add(holder.getId().getPath());
                }
            }
            ok(empty.isEmpty(), rolled + " creature loot tables give something; empty: " + empty);
            for (String chest : List.of("gemstuff", "foodstuff", "elementstuff")) {
                var key = ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("chests/" + chest));
                var items = server().reloadableRegistries().getLootTable(key).getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
                ok(!items.isEmpty(), "chest table " + chest + " gives something");
            }
            var reward = ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("gameplay/tower_flag_reward"));
            ok(!server().reloadableRegistries().getLootTable(reward).getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).isEmpty(), "the flag's reward table gives something");
        });
        once("recipes", () -> {
            ServerLevel level = overworld();
            var manager = server().getRecipeManager();
            ItemStack torch = manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(new ItemStack(Items.TORCH), new ItemStack(ModContent.DRAGONS_EYE.get()))), level)
                    .map(r -> r.value().assemble(CraftingInput.of(2, 1, List.of(new ItemStack(Items.TORCH), new ItemStack(ModContent.DRAGONS_EYE.get()))), level.registryAccess())).orElse(ItemStack.EMPTY);
            ok(torch.is(ModContent.EXTREME_TORCH_ITEM.get()), "a torch and a Dragon's Eye craft an Extreme Torch");
            ItemStack eye = manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(ModContent.DRAGONS_EYE_STONE_ITEM.get())), level)
                    .map(r -> r.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
            ok(eye.is(ModContent.DRAGONS_EYE.get()), "Dragon's Eye Stone smelts to a Dragon's Eye");
            ItemStack banner = new ItemStack(Items.WHITE_BANNER);
            ItemStack gem = new ItemStack(Items.DIAMOND_BLOCK);
            ItemStack air = ItemStack.EMPTY;
            var flagInput = CraftingInput.of(1, 3, List.of(banner, gem, gem));
            ok(manager.getRecipeFor(RecipeType.CRAFTING, flagInput, level).isPresent(), "a banner on two diamond blocks makes a flag");
            ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
            worn.setDamageValue(500);
            var tape = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("orespawn", "duct_tape")));
            var tapeInput = CraftingInput.of(2, 1, List.of(tape, worn));
            ItemStack fixed = manager.getRecipeFor(RecipeType.CRAFTING, tapeInput, level).map(r -> r.value().assemble(tapeInput, level.registryAccess())).orElse(ItemStack.EMPTY);
            ok(fixed.is(Items.DIAMOND_PICKAXE) && fixed.getDamageValue() == 0, "real recipe lookup: duct tape repairs a pickaxe");
            ItemStack gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("orespawn", "ray_gun")));
            ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
            var rayInput = CraftingInput.of(3, 3, List.of(shard, shard, shard, shard, gun, shard, shard, shard, shard));
            ok(manager.getRecipeFor(RecipeType.CRAFTING, rayInput, level).map(r -> r.value().assemble(rayInput, level.registryAccess()).is(ModContent.SHRINK_RAY.get())).orElse(false), "a ray gun and amethyst make the Shrink Ray");
        });
    }

    // ---- the items, used the way a player uses them

    private static void itemChecks() {
        Cow[] cow = new Cow[1];
        once("ray setup", () -> {
            survival();
            BlockPos at = newArena();
            cow[0] = spawn(EntityType.COW, overworld(), at.getX() + 0.5, ARENA_Y, at.getZ() + 8.5);
            hold(ModContent.SHRINK_RAY.get());
        });
        pause(10);
        once("ray fires", () -> {
            ServerPlayer p = player();
            Vec3 aim = cow[0].getEyePosition().subtract(p.getEyePosition());
            float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z)));
            p.moveTo(p.getX(), p.getY(), p.getZ(), yaw, pitch);
            p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            ok(p.getMainHandItem().getDamageValue() == 1, "a Shrink Ray shot costs a charge");
        });
        until("ray hits", 80, () -> cow[0].hasEffect(ModContent.SHRUNK));
        pause(5);
        once("ray result", () -> {
            ok(Math.abs(cow[0].getScale() - 0.25f) < 0.01f, "a real bolt shrank a real cow to " + cow[0].getScale());
            cow[0].kill();
        });
        pause(5);
        once("no drops from shrunk", () -> ok(nearest(ItemEntity.class, 20.0) == null, "a shrunk cow drops nothing"));

        Cow[] target = new Cow[1];
        Vec3[] before = new Vec3[1];
        once("sword in hand", () -> {
            clearAround();
            hold(ModContent.DRAGONS_EYE_SWORD.get());
        });
        pause(3);
        once("sword", () -> {
            ServerPlayer p = player();
            target[0] = spawn(EntityType.COW, p.serverLevel(), p.getX() + 2.0, p.getY(), p.getZ());
            target[0].setHealth(target[0].getMaxHealth());
            target[0].getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0);
            target[0].setHealth(1000.0f);
            before[0] = p.position();
            attack(target[0]);
            before[0] = target[0].position().add(target[0].getBbWidth() / 2.0 + 1.0, 0.0, 0.0);
            ok(p.position().distanceTo(before[0]) > 0.5, "the Dragon's Eye Sword blinked its wielder " + String.format("%.1f", p.position().distanceTo(before[0])) + " blocks");
            ok(target[0].getHealth() < 1000.0f, "and hit for " + (1000.0f - target[0].getHealth()));
            ok(p.getMainHandItem().getDamageValue() >= 1, "and took wear");
        });

        once("staff", () -> {
            clearAround();
            hold(ModContent.DIMENSION_STAFF.get());
            ServerPlayer p = player();
            ResourceKey<Level> from = p.level().dimension();
            p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            ok(player().level().dimension() != from, "the Dimension Staff moved us from " + from.location() + " to " + player().level().dimension().location());
            ok(player().getCooldowns().isOnCooldown(ModContent.DIMENSION_STAFF.get()), "and went on cooldown");
        });
        pause(50);
        once("staff back", () -> {
            ServerPlayer p = player();
            p.setShiftKeyDown(true);
            p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(false);
            ok(player().level().dimension() == Level.OVERWORLD, "sneaking took us back, to " + player().level().dimension().location());
        });
        pause(20);

        // Each elemental staff fired at a live target.
        int[] element = {0};
        for (int i = 0; i < 6; i++) {
            final int index = i;
            Cow[] mark = new Cow[1];
            once("staff " + i + " set up", () -> {
                BlockPos at = newArena();
                hold(ModContent.elementStaffs().get(index));
                mark[0] = spawn(EntityType.COW, overworld(), at.getX() + 0.5, ARENA_Y, at.getZ() + 6.5);
                mark[0].getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0);
                mark[0].setHealth(200.0f);
            });
            pause(15);
            once("staff " + i + " fires", () -> {
                ServerPlayer p = player();
                Vec3 aim = mark[0].getEyePosition().subtract(p.getEyePosition());
                p.moveTo(p.getX(), p.getY(), p.getZ(), (float) Math.toDegrees(Math.atan2(-aim.x, aim.z)), (float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
                p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            });
            until("staff " + i + " hits", 60, () -> mark[0].getHealth() < 200.0f || !mark[0].isAlive());
            once("staff " + i + " result", () -> {
                ok(mark[0].getHealth() < 200.0f, BuiltInRegistries.ITEM.getKey(ModContent.elementStaffs().get(index)).getPath() + " did " + (200.0f - mark[0].getHealth()) + " damage"
                        + (mark[0].getActiveEffects().isEmpty() ? "" : ", effects " + mark[0].getActiveEffects().size()));
                clearAround();
            });
        }
    }

    // ---- every hostile creature against a player standing still

    private static void everyCreatureFights() {
        once("fights start", () -> {
            survival();
            note("fight table: creature | first hit (s) | hits | total damage | biggest hit | closest (blocks) | player max Y");
        });
        Map<String, Integer> distance = new TreeMap<>();
        fight("nastysaurus_prime", ModContent.NASTYSAURUS_PRIME.get(), 20, 400, true);
        fight("needlenose", ModContent.NEEDLENOSE.get(), 10, 300, true);
        fight("ant_queen", ModContent.ANT_QUEEN.get(), 24, 400, true);
        fight("bloodlust_ant", ModContent.BLOODLUST_ANT.get(), 10, 300, true);
        fight("nope", ModContent.NOPE.get(), 24, 400, true);
        fight("baby_nope", ModContent.BABY_NOPE.get(), 8, 300, true);
        fight("chaos_demon", ModContent.CHAOS_DEMON.get(), 12, 300, true);
        fight("baby_sitter", ModContent.BABY_SITTER.get(), 16, 400, true);
        fight("kraken_king", ModContent.KRAKEN_KING.get(), 24, 600, true);
        fight("squid_guard", ModContent.SQUID_GUARD.get(), 12, 300, true);
        fight("dargon", ModContent.DARGON.get(), 40, 600, true);
        fight("element_dragon", ModContent.ELEMENT_DRAGON.get(), 20, 400, true);
        fight("doom_worm", ModContent.DOOM_WORM.get(), 30, 500, true);
        fight("ufo_fighter", ModContent.UFO_FIGHTER.get(), 30, 300, true);
        fight("martian with a bow", ModContent.MARTIAN.get(), 12, 300, true, mob -> martian(mob, false));
        fight("martian with a ray gun", ModContent.MARTIAN.get(), 12, 300, true, mob -> martian(mob, true));
        // The mothership never fires itself: what matters is that its craft come.
        fight("ufo", ModContent.UFO.get(), 30, 400, false);
    }

    private static void martian(Mob mob, boolean rayGun) {
        var tag = new net.minecraft.nbt.CompoundTag();
        mob.addAdditionalSaveData(tag);
        tag.putBoolean("RayGun", rayGun);
        mob.readAdditionalSaveData(tag);
    }

    private static void fight(String name, EntityType<? extends Mob> type, int start, int ticks, boolean mustHurt) {
        fight(name, type, start, ticks, mustHurt, mob -> {
        });
    }

    private static void fight(String name, EntityType<? extends Mob> type, int start, int ticks, boolean mustHurt, java.util.function.Consumer<Mob> prepare) {
        Mob[] enemy = new Mob[1];
        double[] closest = {1e9};
        double[] maxY = {-1e9};
        once(name + " set up", () -> {
            clearAround();
            BlockPos at = newArena();
            holdNothing();
            enemy[0] = spawn(type, overworld(), at.getX() + 0.5 + start, ARENA_Y, at.getZ() + 0.5);
            prepare.accept(enemy[0]);
            hits = 0;
            totalDamage = 0.0f;
            biggestHit = 0.0f;
            firstHitTick = -1;
            fightTicks = 0;
            fightOn = true;
        });
        int[] left = {ticks};
        until(name + " fight", ticks + 2, () -> {
            ServerPlayer p = player();
            if (enemy[0].isAlive()) {
                closest[0] = Math.min(closest[0], enemy[0].distanceTo(p));
            }
            maxY[0] = Math.max(maxY[0], p.getY());
            if (TRACE.contains(name) && left[0] % 40 == 0 && enemy[0].isAlive()) {
                if (enemy[0] instanceof Dargon d) {
                    note(String.format("  dargon activity=%d sit=%s balls=%d y=%.1f hits=%d ground=%s", d.getActivity(), d.isOrderedToSit(),
                            p.serverLevel().getEntitiesOfClass(danger.orespawn.dangerzone.entity.PurpleFireball.class, p.getBoundingBox().inflate(100.0)).size(), d.getY(), hits, d.onGround()));
                }
                note(String.format("  %s t=%d dist=%.1f target=%s nav=%s sees=%s reach=%s aggressive=%s me=%.1f,%.1f it=%.1f,%.1f,%.1f", name, ticks - left[0], enemy[0].distanceTo(p),
                        enemy[0].getTarget() == null ? "none" : enemy[0].getTarget().getType().toShortString(), enemy[0].getNavigation().isDone() ? "done" : "moving",
                        enemy[0].getSensing().hasLineOfSight(p), enemy[0].isWithinMeleeAttackRange(p), enemy[0].isAggressive(), p.getX(), p.getZ(), enemy[0].getX(), enemy[0].getY(), enemy[0].getZ()));
            }
            return --left[0] <= 0;
        });
        once(name + " result", () -> {
            fightOn = false;
            ServerPlayer p = player();
            note(String.format("fight %s | %s | %d | %.0f | %.0f | %.1f | %.0f", name, firstHitTick < 0 ? "never" : String.format("%.1f", firstHitTick / 20.0), hits, totalDamage, biggestHit, closest[0], maxY[0]));
            if (mustHurt) {
                ok(hits > 0, name + " hurt a player standing still within " + ticks / 20 + "s");
            } else {
                long craft = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(160.0), m -> m != enemy[0]).size();
                ok(craft > 0, name + " sent out " + craft + " craft and crew");
            }
            // Dropped from a height by a Kraken, or wherever it was left.
            p.teleportTo(p.serverLevel(), p.getX(), ARENA_Y, p.getZ(), 0.0f, 0.0f);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                p.setItemSlot(slot, ItemStack.EMPTY);
            }
            clearAround();
        });
    }

    // ---- the Princess, by real right-clicks

    private static void princessStages() {
        TamableAnimal[] current = new TamableAnimal[1];
        once("princess", () -> {
            survival();
            clearAround();
            BlockPos at = newArena();
            ServerPlayer p = player();
            TamableAnimal baby = (TamableAnimal) danger.orespawn.dangerzone.entity.PrincessGrowth.baby().create(overworld());
            baby.moveTo(at.getX() + 2.5, ARENA_Y, at.getZ() + 0.5, 0.0f, 0.0f);
            baby.tame(p);
            overworld().addFreshEntity(baby);
            current[0] = baby;
            hold(Items.DIAMOND_BLOCK);
        });
        pause(5);
        once("grow to teen", () -> {
            ServerPlayer p = player();
            p.interactOn(current[0], InteractionHand.MAIN_HAND);
            PrincessTeen teen = nearest(PrincessTeen.class, 16.0);
            ok(teen != null && teen.isOwnedBy(p), "a right-click with a diamond block grew the baby Princess into a Teen");
            current[0] = teen;
            hold(Items.DIAMOND_BLOCK);
        });
        pause(5);
        once("grow to adult", () -> {
            ServerPlayer p = player();
            p.interactOn(current[0], InteractionHand.MAIN_HAND);
            PrincessYoungAdult adult = nearest(PrincessYoungAdult.class, 32.0);
            ok(adult != null && adult.isOwnedBy(p), "and again into a Young Adult");
            current[0] = adult;
            holdNothing();
        });
        pause(5);
        double[] start = new double[3];
        once("ride her", () -> {
            ServerPlayer p = player();
            p.setShiftKeyDown(false);
            p.interactOn(current[0], InteractionHand.MAIN_HAND);
            ok(p.getVehicle() == current[0], "her owner can ride her: vehicle=" + (p.getVehicle() == null ? "none" : p.getVehicle().getName().getString()));
            p.stopRiding();
            hold(Items.DIAMOND);
        });
        pause(5);
        once("shrink back", () -> {
            ServerPlayer p = player();
            p.interactOn(current[0], InteractionHand.MAIN_HAND);
            ok(current[0].isRemoved() && nearest(PrincessTeen.class, 32.0) != null, "a diamond took her back to a Teen");
            clearAround();
        });
    }

    private static void nurseBug() {
        float[] before = new float[1];
        once("nurse", () -> {
            clearAround();
            BlockPos at = newArena();
            spawn(ModContent.NURSE_BUG.get(), overworld(), at.getX() + 8.5, ARENA_Y + 3.0, at.getZ() + 0.5);
            nurseStart = true;
        });
        until("nurse heals", 400, () -> {
            ServerPlayer p = player();
            // Held below full by the test: the driver tops the player up each tick, so count hearts instead.
            p.setHealth(2.0f);
            return p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        });
        once("nurse result", () -> {
            ok(player().hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "a Nurse Bug flew to a hurt player and stung them better");
            clearAround();
        });
    }

    private static boolean nurseStart;

    // ---- a trophy of every creature in the mod and the port, drawn

    private static void trophiesOfEverything() {
        int[] placed = new int[1];
        once("trophies", () -> {
            survival();
            clearAround();
            BlockPos at = newArena();
            ServerLevel level = overworld();
            int column = 0;
            int row = 0;
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (!(id.getNamespace().equals("orespawn") || id.getNamespace().equals(OreSpawnDangerZone.MOD_ID))) {
                    continue;
                }
                if (!(type.create(level) instanceof Mob) || type.is(ModContent.NO_TROPHY)) {
                    continue;
                }
                Trophy trophy = ModContent.TROPHY_ENTITY.get().create(level);
                trophy.moveTo(at.getX() + 1.5 + column * 1.5, ARENA_Y, at.getZ() + 3.5 + row * 1.5, 0.0f, 0.0f);
                trophy.setSubject(id.toString());
                level.addFreshEntity(trophy);
                placed[0]++;
                if (++column >= 16) {
                    column = 0;
                    row++;
                }
            }
            player().teleportTo(level, at.getX() + 13.0, ARENA_Y + 18.0, at.getZ() - 6.0, 0.0f, 50.0f);
            player().getAbilities().mayfly = true;
            player().getAbilities().flying = true;
            player().onUpdateAbilities();
        });
        pause(120);
        once("trophies result", () -> {
            Minecraft.getInstance().execute(() -> net.minecraft.client.Screenshot.grab(Minecraft.getInstance().gameDirectory, "dzt_trophies.png", Minecraft.getInstance().getMainRenderTarget(), m -> {
            }));
            note(placed[0] + " trophies placed and drawn for 6 seconds; any that couldn't draw log \"Can't draw a trophy\"");
            ok(placed[0] > 100, "there's a trophy for each of " + placed[0] + " creatures");
        });
        pause(20);
        once("trophy pickup", () -> {
            ServerPlayer p = player();
            Trophy one = nearest(Trophy.class, 64.0);
            String subject = one.subject();
            p.teleportTo(p.serverLevel(), one.getX() + 1.0, one.getY(), one.getZ(), 0.0f, 0.0f);
            p.resetAttackStrengthTicker();
            p.attack(one);
            ok(one.isRemoved(), "hitting a trophy pops it off");
            ItemEntity dropped = nearest(ItemEntity.class, 6.0);
            ok(dropped != null && TrophyItem.subject(dropped.getItem()).equals(subject), "and it comes back as its own item, " + (dropped == null ? "nothing" : dropped.getItem().getHoverName().getString()));
            if (dropped != null) {
                BlockPos ground = one.blockPosition();
                ItemStack stack = dropped.getItem().copy();
                dropped.discard();
                p.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var result = p.gameMode.useItemOn(p, p.serverLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground.below()).add(0.0, 0.5, 0.0), Direction.UP, ground.below(), false));
                ok(result.consumesAction(), "and can be placed again");
            }
            clearAround();
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        });
    }

    // ---- the whole flag ladder, sped up

    private static void flagLadder() {
        TowerFlag[] flag = new TowerFlag[1];
        int total = TowerFlag.ladder().size();
        int[] seconds = new int[1];
        BlockPos[] flagArena = new BlockPos[1];
        once("flag arena", () -> {
            survival();
            clearAround();
            flagArena[0] = newArena();
        });
        pause(20);
        once("flag", () -> {
            BlockPos at = flagArena[0];
            seconds[0] = DzConfig.FLAG_WAVE_SECONDS.get();
            DzConfig.FLAG_WAVE_SECONDS.set(5);
            ServerPlayer p = player();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.TOWER_FLAG_ITEM.get()));
            BlockPos ground = at.below();
            var result = p.gameMode.useItemOn(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground).add(5.0, 0.5, 0.0), Direction.UP, ground.east(5), false));
            flag[0] = nearest(TowerFlag.class, 16.0);
            ok(flag[0] != null, "using the flag on the ground plants it");
            ok(p.getMainHandItem().isEmpty(), "and uses it up");
            float before = flag[0].getHealth();
            hold(Items.DIAMOND_SWORD);
            attack(flag[0]);
            ok(flag[0].getHealth() == before, "a player can't hurt it during the event");
            p.teleportTo(p.serverLevel(), flag[0].getX(), ARENA_Y + 40.0, flag[0].getZ() - 70.0, 0.0f, 20.0f);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        });
        double[] worstTick = {0.0};
        int[] rung = {0};
        int[] maxMobs = {0};
        until("flag ladder", (total + 3) * 120, () -> {
            worstTick[0] = Math.max(worstTick[0], server().getCurrentSmoothedTickTime());
            ServerPlayer p = player();
            if (flag[0].rung() != rung[0]) {
                rung[0] = flag[0].rung();
                // Clears the older rungs, the way a player's defences would, so a sped-up ladder doesn't drown the server.
                var mobs = p.serverLevel().getEntitiesOfClass(Mob.class, flag[0].getBoundingBox().inflate(96.0), m -> m != flag[0] && m.tickCount > 300);
                maxMobs[0] = Math.max(maxMobs[0], mobs.size());
                mobs.forEach(Mob::discard);
                if (rung[0] > 0 && rung[0] % 10 == 0) {
                    note(String.format("flag rung %d of %d, health %.0f, worst tick %.0f ms", rung[0], total, flag[0].getHealth(), worstTick[0]));
                }
            }
            if (flag[0].getHealth() < 500.0f) {
                flag[0].setHealth(flag[0].getMaxHealth());
            }
            return flag[0].finished();
        });
        once("flag result", () -> {
            DzConfig.FLAG_WAVE_SECONDS.set(seconds[0]);
            ok(flag[0].finished(), "the flag got through all " + total + " rungs of its ladder");
            note(String.format("flag: worst smoothed tick %.0f ms over the sped-up ladder", worstTick[0]));
            ServerPlayer p = player();
            var paid = p.serverLevel().getEntitiesOfClass(ItemEntity.class, flag[0].getBoundingBox().inflate(8.0));
            ok(paid.stream().anyMatch(e -> e.getItem().is(ModContent.TOWER_FLAG_ITEM.get())), "it gave the flag back");
            ok(paid.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND) || e.getItem().is(Items.EMERALD)), "and paid out: " + paid.size() + " stacks");
            ok(flag[0].isRemoved(), "and the pole came down");
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            clearAround();
        });
    }

    // ---- the Kraken King's chain, end to end, by a real player's hands

    private static void krakenChain() {
        ChaosDemon[] demon = new ChaosDemon[1];
        BabySitter[] sitter = new BabySitter[1];
        KrakenKing[] king = new KrakenKing[1];
        once("chain start", () -> {
            survival();
            clearAround();
            BlockPos at = newArena();
            hold(Items.DIAMOND_SWORD);
            demon[0] = spawn(ModContent.CHAOS_DEMON.get(), overworld(), at.getX() + 4.5, ARENA_Y, at.getZ() + 0.5);
        });
        pause(10);
        once("demon teleports", () -> {
            ServerPlayer p = player();
            Vec3 from = demon[0].position();
            p.resetAttackStrengthTicker();
            p.teleportTo(p.serverLevel(), demon[0].getX() + 2.0, demon[0].getY(), demon[0].getZ(), 90.0f, 0.0f);
            p.attack(demon[0]);
            ok(demon[0].getHealth() < demon[0].getMaxHealth(), "a sword hurt the Chaos Demon");
            ok(demon[0].position().distanceTo(from) > 0.1, String.format("and it teleported when hit, %.1f blocks", demon[0].position().distanceTo(from)));
        });
        pause(10);
        once("demon dies", () -> {
            demon[0].invulnerableTime = 0;
            demon[0].setHealth(1.0f);
            attack(demon[0]);
            ok(!demon[0].isAlive(), "a player's blow killed the Chaos Demon");
        });
        until("sitter arrives", 260, () -> {
            sitter[0] = nearest(BabySitter.class, 48.0);
            return sitter[0] != null;
        });
        once("sitter", () -> {
            ok(sitter[0] != null && sitter[0].getTarget() == player(), "seven and a half seconds later the Baby Sitter came for the killer");
        });
        pause(40);
        once("sitter dies", () -> {
            sitter[0].invulnerableTime = 0;
            sitter[0].setHealth(1.0f);
            attack(sitter[0]);
            ok(!sitter[0].isAlive(), "a player's blow killed the Baby Sitter");
        });
        until("warp", 260, () -> player().level().dimension() == DzPlaces.KRAKEN_REALM);
        once("warped", () -> {
            ServerPlayer p = player();
            ok(p.level().dimension() == DzPlaces.KRAKEN_REALM, "seven and a half seconds later the player was warped to " + p.level().dimension().location());
            note("landed at " + p.blockPosition().toShortString() + ", in " + p.level().getBlockState(p.blockPosition()).getBlock());
            BlockPos under = p.blockPosition();
            ok(p.level().noCollision(p), "not stuck in a block");
        });
        until("castle built", 1200, () -> DzBuilder.activeCount() == 0);
        BlockPos[] spawner = new BlockPos[1];
        once("castle", () -> {
            ServerPlayer p = player();
            ServerLevel realm = p.serverLevel();
            var castle = DzPlaces.nearestCastle(p.getX(), p.getZ());
            int gems = 0;
            int food = 0;
            int staffs = 0;
            int filled = 0;
            for (int dx = -2; dx < 60; dx++) {
                for (int dz = -2; dz < 50; dz++) {
                    for (int y = 20; y < 110; y++) {
                        BlockPos pos = new BlockPos(castle.getMinBlockX() + dx, y, castle.getMinBlockZ() + dz);
                        var state = realm.getBlockState(pos);
                        if (state.is(ModContent.KRAKEN_KING_SPAWNER.get())) {
                            spawner[0] = pos;
                        }
                        if (state.is(Blocks.CHEST) && realm.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                            chest.unpackLootTable(p);
                            boolean any = false;
                            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                                ItemStack stack = chest.getItem(slot);
                                any |= !stack.isEmpty();
                                if (stack.getItem() instanceof danger.orespawn.dangerzone.item.ElementStaffItem) {
                                    staffs++;
                                }
                            }
                            filled += any ? 1 : 0;
                            gems++;
                        }
                    }
                }
            }
            ok(gems == 96 && filled == 96, "the castle has " + gems + " chests, " + filled + " with something in them");
            note("elemental staffs across this castle's chests: " + staffs);
            ok(staffs <= 8, "a castle's chests hold a handful of staffs, here " + staffs + ", not forty");
            ok(spawner[0] != null, "and something that wakes the King, at " + (spawner[0] == null ? "nowhere" : spawner[0].toShortString()));
            ok(realm.getEntitiesOfClass(BabySitter.class, new AABB(castle.getMinBlockX() - 64, 0, castle.getMinBlockZ() - 64, castle.getMinBlockX() + 120, 200, castle.getMinBlockZ() + 120)).size() == 3, "and its three Baby Sitters");
        });
        once("wake the King", () -> {
            ServerPlayer p = player();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            p.teleportTo(p.serverLevel(), spawner[0].getX() + 0.5, spawner[0].getY() + 1.0, spawner[0].getZ() + 0.5, 0.0f, 90.0f);
            p.gameMode.destroyBlock(spawner[0]);
            ok(p.serverLevel().getBlockState(spawner[0]).isAir(), "breaking the block removed it");
            king[0] = nearest(KrakenKing.class, 48.0);
            ok(king[0] != null, "and woke the Kraken King");
        });
        once("no double king", () -> ok(player().serverLevel().getEntitiesOfClass(KrakenKing.class, player().getBoundingBox().inflate(64.0)).size() == 1, "just the one King"));
        pause(40);
        once("hit the King", () -> {
            hold(Items.DIAMOND_SWORD);
            king[0].invulnerableTime = 0;
            attack(king[0]);
            ok(king[0].getHealth() < king[0].getMaxHealth(), "a sword hurt the King");
            ok(nearest(PrincessYoungAdult.class, 64.0) != null, "and the Young Adult Princess came to his side");
        });
        pause(200);
        // A Kraken shrugs off anything within 30 ticks of its last hit, the port's rule: so swing until a blow lands.
        until("kill the King", 60, () -> {
            player().getAbilities().flying = false;
            for (var e : player().serverLevel().getEntitiesOfClass(LivingEntity.class, player().getBoundingBox().inflate(120.0), e -> e != king[0] && !(e instanceof Player))) {
                e.discard();
            }
            king[0].invulnerableTime = 0;
            king[0].setHealth(1.0f);
            attack(king[0]);
            return !king[0].isAlive();
        });
        once("the King is dead", () -> ok(!king[0].isAlive(), "a player's blow killed the King"));
        pause(40);
        once("no more kings", () -> ok(player().serverLevel().getEntitiesOfClass(KrakenKing.class, player().getBoundingBox().inflate(128.0)).isEmpty(), "and no more Kings came"));
        ItemEntity[] staff = new ItemEntity[1];
        once("find the staff", () -> {
            ServerPlayer p = player();
            staff[0] = p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(120.0), e -> e.getItem().is(ModContent.DIMENSION_STAFF.get())).stream().findFirst().orElse(null);
            ok(staff[0] != null, "he dropped the Dimension Staff");
            holdNothing();
            if (staff[0] != null) {
                p.teleportTo(p.serverLevel(), staff[0].getX(), staff[0].getY(), staff[0].getZ(), 0.0f, 0.0f);
                staff[0].setNoPickUpDelay();
            }
        });
        until("pick it up", 60, () -> has(ModContent.DIMENSION_STAFF.get()));
        once("leave", () -> {
            ServerPlayer p = player();
            ok(has(ModContent.DIMENSION_STAFF.get()), "the player picked it up");
            int slot = p.getInventory().findSlotMatchingItem(new ItemStack(ModContent.DIMENSION_STAFF.get()));
            if (slot >= 0) {
                p.setItemInHand(InteractionHand.MAIN_HAND, p.getInventory().getItem(slot).copy());
                p.getInventory().setItem(slot, ItemStack.EMPTY);
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.DIMENSION_STAFF.get()));
                p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            }
            ok(player().level().dimension() != DzPlaces.KRAKEN_REALM, "and left the realm with it, for " + player().level().dimension().location());
        });
    }

    // ---- a castle whose chunk loads as a player walks up, far from the first and at negative coordinates

    private static void walkToACastle() {
        int x = -99 * 16;
        int z = -99 * 16;
        boolean[] started = new boolean[1];
        once("walk to a castle", () -> {
            survival();
            ServerPlayer p = player();
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(p, level(DzPlaces.KRAKEN_REALM), x - 300, 130, z);
        });
        pause(40);
        until("fly in", 900, () -> {
            ServerPlayer p = player();
            p.teleportTo(p.getX() + 2.0, 130.0, p.getZ());
            started[0] |= DzBuilder.activeCount() > 0;
            return started[0];
        });
        until("second castle built", 1200, () -> DzBuilder.activeCount() == 0);
        pause(40);
        once("second castle", () -> {
            ok(started[0], "a castle started going up when its chunk loaded");
            ServerPlayer p = player();
            p.teleportTo(x + 30.0, 130.0, z + 20.0);
        });
        pause(60);
        once("second castle result", () -> {
            ServerPlayer p = player();
            int sitters = p.serverLevel().getEntitiesOfClass(BabySitter.class, new AABB(x - 64, 0, z - 64, x + 120, 200, z + 120)).size();
            ok(sitters == 3, "and it has its three Baby Sitters: " + sitters);
            clearAround();
            danger.orespawn.dangerzone.item.DimensionStaffItem.jump(p, overworld());
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        });
        pause(20);
    }

    // ---- Dragon's Lair, from the ant through the ritual to riding Dargon

    private static void lairPlaythrough() {
        BlockPos[] eye = new BlockPos[1];
        BlockPos[] pad = new BlockPos[1];
        Dargon[] dargon = new Dargon[1];
        var drant = new danger.orespawn.dangerzone.entity.PortalAnt[1];
        once("drant", () -> {
            survival();
            ServerLevel level = overworld();
            player().teleportTo(level, nextArena + 400.5, ARENA_Y, 0.5, 0.0f, 0.0f);
            nextArena += 400;
            drant[0] = spawn(ModContent.DRANT.get(), level, nextArena + 1.5, ARENA_Y, 0.5);
            holdNothing();
        });
        pause(10);
        once("right-click the Drant", () -> {
            ServerPlayer p = player();
            p.interactOn(drant[0], InteractionHand.MAIN_HAND);
            ok(player().level().dimension() == DzPlaces.DRAGONS_LAIR, "right-clicking a Drant went to " + player().level().dimension().location());
        });
        pause(60);
        once("the fossils", () -> {
            ServerPlayer p = player();
            ServerLevel lair = p.serverLevel();
            int fossils = 0;
            int eyes = 0;
            int dark = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int cx = p.getBlockX();
            int cz = p.getBlockZ();
            for (int dx = -32; dx < 32; dx++) {
                for (int dz = -32; dz < 32; dz++) {
                    for (int y = 0; y < 46; y++) {
                        pos.set(cx + dx, y, cz + dz);
                        var state = lair.getBlockState(pos);
                        if (state.is(ModContent.FOSSIL_BONE.get())) {
                            fossils++;
                        } else if (state.is(ModContent.DRAGONS_EYE_STONE.get())) {
                            eyes++;
                            if (eye[0] == null) {
                                eye[0] = pos.immutable();
                            }
                        } else if (state.is(ModContent.DARK_STONE.get())) {
                            dark++;
                        }
                    }
                }
            }
            note("around the arrival: " + dark + " Dark Stone, " + fossils + " Fossilized Bone, " + eyes + " Dragon's Eye Stone");
            ok(dark > 1000, "the Lair is made of Dark Stone");
            ok(fossils > 0, "and has fossils");
            ok(eyes > 0, "with Dragon's Eye Stone in them");
        });
        once("mine an eye stone", () -> {
            ServerPlayer p = player();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            p.teleportTo(p.serverLevel(), eye[0].getX() + 0.5, eye[0].getY() + 1.0, eye[0].getZ() + 0.5, 0.0f, 90.0f);
            p.gameMode.destroyBlock(eye[0]);
        });
        until("eye stone drops", 40, () -> {
            ItemEntity item = nearest(ItemEntity.class, 6.0);
            return item != null && item.getItem().is(ModContent.DRAGONS_EYE_STONE_ITEM.get());
        });
        once("eye stone result", () -> ok(nearest(ItemEntity.class, 6.0) != null, "mining one with a pickaxe drops it"));

        once("the ritual", () -> {
            ServerPlayer p = player();
            ServerLevel lair = p.serverLevel();
            int y = 110;
            lair.getChunk(new BlockPos(0, y, 0));
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    lair.setBlockAndUpdate(new BlockPos(dx, y - 2, dz), ModContent.DARK_STONE.get().defaultBlockState());
                }
            }
            p.teleportTo(lair, 0.5, y - 1.0, -2.5, 0.0f, 45.0f);
            // Nine stones by a player's own hand, and the torch last.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ItemStack stone = new ItemStack(ModContent.DRAGONS_EYE_STONE_ITEM.get());
                    p.setItemInHand(InteractionHand.MAIN_HAND, stone);
                    BlockPos below = new BlockPos(dx, y - 2, dz);
                    p.gameMode.useItemOn(p, lair, stone, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0.0, 0.5, 0.0), Direction.UP, below, false));
                }
            }
            BlockPos centre = new BlockPos(0, y - 1, 0);
            ok(lair.getBlockState(centre).is(ModContent.DRAGONS_EYE_STONE.get()), "the pad went down by hand");
            ItemStack torch = new ItemStack(ModContent.EXTREME_TORCH_ITEM.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, torch);
            p.gameMode.useItemOn(p, lair, torch, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(centre).add(0.0, 0.5, 0.0), Direction.UP, centre, false));
            pad[0] = centre.above();
            note("torch placed at " + pad[0].toShortString() + ": " + lair.getBlockState(pad[0]).getBlock());
        });
        until("the carve starts", 40, () -> DzBuilder.activeCount() > 0);
        double[] worst = {0.0};
        double[] lowest = {1e9};
        until("the carve finishes", 1600, () -> {
            worst[0] = Math.max(worst[0], server().getCurrentSmoothedTickTime());
            lowest[0] = Math.min(lowest[0], player().getY());
            return DzBuilder.activeCount() == 0;
        });
        pause(40);
        once("the lair", () -> {
            ServerPlayer p = player();
            ServerLevel lair = p.serverLevel();
            note(String.format("carve: worst smoothed tick %.0f ms; the player's lowest point %.0f (platform %d)", worst[0], lowest[0], pad[0].getY()));
            ok(lowest[0] >= pad[0].getY() - 1.0, "the player didn't fall during the carve");
            ok(lair.getBlockState(pad[0].offset(30, 20, 50)).isAir(), "the void is open");
            ok(lair.getBlockState(pad[0].offset(0, 0, 50)).is(ModContent.DARGON_STONE_DARK.get()), "the bridge is there");
            ok(lair.getBlockState(pad[0].offset(3, 0, 3)).is(ModContent.DARGON_STONE_DARK.get()), "and the platform");
            AABB box = new AABB(pad[0]).inflate(120.0, 80.0, 260.0);
            long chests = 0;
            for (int dx = -40; dx <= 40; dx++) {
                for (int dz = 90; dz <= 160; dz++) {
                    for (int dy = -20; dy <= 60; dy++) {
                        if (lair.getBlockState(pad[0].offset(dx, dy, dz)).is(Blocks.CHEST)) {
                            chests++;
                        }
                    }
                }
            }
            note("Dargon's castle has " + chests + " chests");
            ok(chests >= 4, "with its food chests");
            ok(lair.getEntitiesOfClass(ElementDragon.class, box).size() >= 6, "six Element Dragons guard it");
            java.util.Set<Integer> elements = new java.util.HashSet<>();
            lair.getEntitiesOfClass(ElementDragon.class, box).forEach(d -> elements.add(d.element()));
            ok(elements.size() == 6, "one of each element: " + elements);
            dargon[0] = lair.getEntitiesOfClass(Dargon.class, box).stream().findFirst().orElse(null);
            ok(dargon[0] != null, "and Dargon is there");
        });
        once("tame Dargon", () -> {
            ServerPlayer p = player();
            lairMobsAway(dargon[0]);
            holdNothing();
            p.teleportTo(p.serverLevel(), dargon[0].getX(), dargon[0].getY() + 22.0, dargon[0].getZ(), 0.0f, 0.0f);
            p.interactOn(dargon[0], InteractionHand.MAIN_HAND);
            ok(!dargon[0].isTame(), "an empty hand doesn't tame him at full health");
            dargon[0].setHealth(dargon[0].getMaxHealth() * 0.2f);
            p.interactOn(dargon[0], InteractionHand.MAIN_HAND);
            ok(dargon[0].isTame(), "under a quarter health it does");
            ok(player().getVehicle() == dargon[0], "and he carries his tamer");
        });
        pause(40);
        Vec3[] from = new Vec3[1];
        once("fly Dargon", () -> {
            from[0] = dargon[0].position();
            ok(player().getVehicle() == dargon[0], "still riding after two seconds");
            holdKeys = true;
        });
        pause(120);
        once("flew", () -> {
            holdKeys = false;
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().options.keyUp.setDown(false);
                Minecraft.getInstance().options.keyJump.setDown(false);
            });
            double moved = dargon[0].position().distanceTo(from[0]);
            note(String.format("riding Dargon with the forward key for 6 seconds moved him %.1f blocks; client forward=%.2f, client in control=%s", moved, clientForward, clientRiding));
            ok(moved > 3.0, "a rider can steer him");
            player().stopRiding();
            dargon[0].discard();
            clearAround();
        });
    }

    private static void lairMobsAway(Entity keep) {
        ServerPlayer p = player();
        p.serverLevel().getEntitiesOfClass(ElementDragon.class, p.getBoundingBox().inflate(400.0)).forEach(Entity::discard);
    }

    // ---- Doom: the worm hunting a real player

    private static void doomPlaythrough() {
        DoomWorm[] worm = new DoomWorm[1];
        int[] damageBefore = new int[1];
        once("to Doom", () -> {
            survival();
            hold(ModContent.WORM_FOOD.get());
            danger.orespawn.dangerzone.item.DimensionStaffItem.jump(player(), level(DzPlaces.DOOM));
            ok(player().level().dimension() == DzPlaces.DOOM, "the Doom dimension");
        });
        pause(40);
        once("worm with food", () -> {
            ServerPlayer p = player();
            ServerLevel doom = p.serverLevel();
            note("doom ground at the player: " + doom.getBlockState(p.blockPosition().below()).getBlock());
            ok(doom.getBlockState(p.blockPosition().below()).is(ModContent.DOOM_SAND.get()), "it's made of Doom Sand");
            worm[0] = spawn(ModContent.DOOM_WORM.get(), doom, p.getX() + 20.0, 45.0, p.getZ());
            hits = 0;
            fightOn = true;
            fightTicks = 0;
        });
        pause(600);
        once("worm ignored food", () -> {
            ok(hits == 0, "holding Worm Food, thirty seconds next to a Doom Worm did nothing: " + hits + " hits");
            ok(worm[0].isAlive(), "and it lived through thirty seconds of its own desert, at " + worm[0].blockPosition().toShortString());
            ServerPlayer p = player();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            hits = 0;
        });
        until("worm bites", 600, () -> {
            ServerPlayer p = player();
            boolean stripped = p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    || p.getItemBySlot(EquipmentSlot.LEGS).isEmpty() || p.getItemBySlot(EquipmentSlot.FEET).isEmpty() || p.getMainHandItem().isEmpty();
            return hits > 0 && stripped;
        });
        once("worm result", () -> {
            ServerPlayer p = player();
            fightOn = false;
            ok(hits > 0, "without it the worm bit: " + hits + " hits");
            ok(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || p.getItemBySlot(EquipmentSlot.CHEST).isEmpty() || p.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                    || p.getItemBySlot(EquipmentSlot.FEET).isEmpty() || p.getMainHandItem().isEmpty(), "and tore off some of the player's gear");
            ok(!p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(20.0), e -> e.getItem().getItem() == Items.DIAMOND_HELMET || e.getItem().getItem() == Items.DIAMOND_CHESTPLATE
                    || e.getItem().getItem() == Items.DIAMOND_LEGGINGS || e.getItem().getItem() == Items.DIAMOND_BOOTS || e.getItem().getItem() == Items.DIAMOND_SWORD).isEmpty(), "which is lying on the ground");
            worm[0].invulnerableTime = 0;
            int before = 0;
            hold(Items.DIAMOND_SWORD);
            float health = worm[0].getHealth();
            attack(worm[0]);
            ok(worm[0].getHealth() < health, "a sword hurts the worm");
        });
        once("worm meets bedrock", () -> {
            ServerPlayer p = player();
            ServerLevel doom = p.serverLevel();
            BlockPos head = worm[0].blockPosition();
            doom.setBlockAndUpdate(head.above(), Blocks.BEDROCK.defaultBlockState());
        });
        until("worm dies", 60, () -> !worm[0].isAlive());
        once("worm gone", () -> {
            ok(!worm[0].isAlive(), "bedrock ended the worm");
            ok(player().serverLevel().getEntitiesOfClass(ItemEntity.class, player().getBoundingBox().inflate(250.0), e -> e.getItem().is(ModContent.DOOM_WORM_EGG.get())).isEmpty(), "and it left no eggs");
        });
        pause(20);
        // Doom's sand rain: a big enough hole gets rained into.
        once("dig a trench", () -> {
            ServerPlayer p = player();
            clearAround();
            ServerLevel doom = p.serverLevel();
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(doom, p.getX(), 60.0, p.getZ(), 0.0f, 0.0f);
            for (int dx = -48; dx <= 48; dx++) {
                for (int dz = -48; dz <= 48; dz++) {
                    doom.setBlock(new BlockPos(p.getBlockX() + dx, 49, p.getBlockZ() + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        });
        boolean[] rained = new boolean[1];
        until("sand rain", 1400, () -> {
            ServerPlayer p = player();
            rained[0] = !p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, p.getBoundingBox().inflate(128.0, 256.0, 128.0)).isEmpty();
            return rained[0];
        });
        once("sand rain result", () -> {
            ok(rained[0], "sand rains into holes in the desert");
            player().getAbilities().mayfly = false;
            player().getAbilities().flying = false;
            player().onUpdateAbilities();
        });
    }

    private static void portalAnts() {
        var ant = new danger.orespawn.dangerzone.entity.PortalAnt[1];
        once("back with an ant", () -> {
            holdNothing();
            ServerPlayer p = player();
            ant[0] = spawn(ModContent.DOOM_ANT.get(), p.serverLevel(), p.getX() + 1.0, p.getY(), p.getZ());
        });
        pause(10);
        once("doom ant returns", () -> {
            ServerPlayer p = player();
            ResourceKey<Level> before = p.level().dimension();
            p.teleportTo(p.serverLevel(), ant[0].getX() + 1.0, ant[0].getY(), ant[0].getZ(), 0.0f, 0.0f);
            p.interactOn(ant[0], InteractionHand.MAIN_HAND);
            ok(before == DzPlaces.DOOM && player().level().dimension() == Level.OVERWORLD, "a Doom Ant in Doom took us back to the overworld, from " + before.location() + " to " + player().level().dimension().location());
        });
        pause(20);
        once("doom ant goes", () -> {
            ServerPlayer p = player();
            var again = spawn(ModContent.DOOM_ANT.get(), p.serverLevel(), p.getX() + 1.0, p.getY(), p.getZ());
            p.interactOn(again, InteractionHand.MAIN_HAND);
            ok(player().level().dimension() == DzPlaces.DOOM, "and one in the overworld goes to Doom");
        });
        pause(20);
        once("home", () -> danger.orespawn.dangerzone.item.DimensionStaffItem.jump(player(), overworld()));
        pause(20);
    }

    // ---- that advancements really fire from real play

    private static void advancementsFire() {
        once("advancements", () -> {
            survival();
            clearAround();
            ServerPlayer p = player();
            long total = server().getAdvancements().getAllAdvancements().stream().filter(a -> a.id().getNamespace().equals(OreSpawnDangerZone.MOD_ID)).count();
            ok(total >= 160, total + " advancements loaded");
            var board = server().getAdvancements();
            ok(board.get(OreSpawnDangerZone.id("things_to_do/slayer/prime")) != null, "there's one for the Prime");
            p.getInventory().add(TrophyItem.of("orespawn_dangerzone:nope"));
            p.inventoryMenu.broadcastChanges();
            hold(Items.DIAMOND_SWORD);
        });
        pause(3);
        once("earn it", () -> {
            ServerPlayer p = player();
            var board = server().getAdvancements();
            AdvancementHolder prime = board.get(OreSpawnDangerZone.id("things_to_do/slayer/prime"));
            var trophy = board.get(OreSpawnDangerZone.id("things_to_do/finders/trophy"));
            var mob = spawn(ModContent.NASTYSAURUS_PRIME.get(), p.serverLevel(), p.getX() + 3.0, p.getY(), p.getZ());
            mob.setHealth(1.0f);
            attack(mob);
            ok(!mob.isAlive(), "a Prime killed by hand");
            ok(p.getAdvancements().getOrStartProgress(prime).isDone(), "earned Prime Time");
            if (trophy != null) {
                ok(p.getAdvancements().getOrStartProgress(trophy).isDone(), "and holding a Trophy earned Bragging Rights");
            }
            AdvancementHolder root = board.get(OreSpawnDangerZone.id("things_to_do/root"));
            ok(root != null && p.getAdvancements().getOrStartProgress(root).isDone(), "the Things To Do tab is open");
            clearAround();
        });
    }

    // ---- natural spawning: stand in each place and count what turns up

    private static void naturalSpawning() {
        spawnCheck("orespawn:mining", List.of("nastysaurus_prime", "needlenose", "nurse_bug", "volcano"));
        spawnCheck("orespawn:chaos", List.of("nastysaurus_prime", "ant_queen", "bloodlust_ant", "nope", "chaos_demon", "volcano", "doom_worm"));
        spawnCheck("orespawn_dangerzone:kraken_realm", List.of("squid_guard", "doom_ant"), 4);
        spawnCheck("orespawn_dangerzone:dragons_lair", List.of("drant"));
        spawnCheck("orespawn_dangerzone:doom", List.of("doom_worm", "doom_ant"));
        extraSpawns("orespawn:chaos", List.of("ufo", "dargon"));
        extraSpawns("orespawn:village", List.of("ufo"));
        extraSpawns("orespawn_dangerzone:kraken_realm", List.of("kraken", "attack_squid"));
        extraSpawns("orespawn_dangerzone:dragons_lair", List.of("terrible_terror"));
        once("spawn check over", () -> {
            player().getAbilities().mayfly = false;
            player().getAbilities().flying = false;
            player().onUpdateAbilities();
            danger.orespawn.dangerzone.item.DimensionStaffItem.jump(player(), overworld());
            overworld().setDayTime(6000L);
        });
        pause(20);
    }

    /** The creatures ExtraSpawns places. Asks it for one of each and looks for what arrived. */
    private static void extraSpawns(String dimension, List<String> names) {
        once("go to " + dimension + " for extras", () -> {
            survival();
            ServerLevel level = server().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
            nextArena += 900;
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player(), level, nextArena, level.getMaxBuildHeight() - 40, 0);
            player().getAbilities().mayfly = true;
            player().getAbilities().flying = true;
            player().onUpdateAbilities();
        });
        pause(100);
        once("down to the surface in " + dimension, () -> {
            ServerPlayer p = player();
            p.teleportTo(p.getX(), p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ()) + 3.0, p.getZ());
        });
        pause(40);
        once("ask for extras in " + dimension, () -> {
            ServerPlayer p = player();
            ServerLevel level = p.serverLevel();
            var entries = danger.orespawn.dangerzone.world.ExtraSpawns.entriesAt(level, p.blockPosition());
            for (String name : names) {
                var entry = entries.stream().filter(e -> e.type().getPath().equals(name)).findFirst().orElse(null);
                if (entry == null) {
                    ok(false, name + " has an entry in " + dimension);
                    continue;
                }
                boolean placed = false;
                for (int i = 0; i < 40 && !placed; i++) {
                    placed = danger.orespawn.dangerzone.world.ExtraSpawns.spawnNear(level, p, entry);
                }
                ok(placed, name + " was placed in " + dimension);
                // A second can't be, once the cap's reached.
                int cap = entry.cap();
                for (int i = 0; i < 200; i++) {
                    danger.orespawn.dangerzone.world.ExtraSpawns.spawnNear(level, p, entry);
                }
                int count = level.getEntities(typeNamed(name), p.getBoundingBox().inflate(160.0), Entity::isAlive).size();
                ok(count <= cap, "and its cap of " + cap + " held: " + count);
            }
        });
        pause(40);
        once("extras lived in " + dimension, () -> {
            ServerPlayer p = player();
            for (String name : names) {
                int count = p.serverLevel().getEntities(typeNamed(name), p.getBoundingBox().inflate(200.0), Entity::isAlive).size();
                ok(count > 0, name + " is still there two seconds on");
            }
            clearAround();
        });
    }

    private static EntityType<?> typeNamed(String name) {
        ResourceLocation own = OreSpawnDangerZone.id(name);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(own) ? BuiltInRegistries.ENTITY_TYPE.get(own) : BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("orespawn", name));
    }

    /**
     * Whether each creature can really turn up there: on the biome's list, where its placement rule says yes, and where
     * it fits. By night and by day, so that the Prime's dark and the Nurse Bug's sun both get their turn. Bosses are too
     * rare to wait for; the game's own spawn loop asks exactly these questions.
     */
    private static void spawnCheck(String dimension, List<String> names) {
        spawnCheck(dimension, names, 1);
    }

    /** More than one spot for a place where any one of them may be open sea. */
    private static void spawnCheck(String dimension, List<String> names, int spots) {
        Map<String, int[]> found = new TreeMap<>();
        for (int spot = 0; spot < spots; spot++) {
            once("go to " + dimension, () -> {
                survival();
                ServerLevel level = server().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
                nextArena += 900;
                danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player(), level, nextArena, level.getMaxBuildHeight() - 40, 0);
                player().getAbilities().mayfly = true;
                player().getAbilities().flying = true;
                player().onUpdateAbilities();
                player().teleportTo(player().getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player().getBlockX(), player().getBlockZ()) + 6.0, player().getZ());
                overworld().setDayTime(18000L);
                // A Kraken's storm isn't day, however high the sun: a Nurse Bug is right not to come out in one.
                overworld().setWeatherParameters(0, 0, false, false);
            });
            pause(80);
            once("night check " + dimension, () -> spawnTries(names, found));
            once("to day", () -> overworld().setDayTime(6000L));
            pause(120);
            once("day check " + dimension, () -> spawnTries(names, found));
        }
        once("spawn verdict " + dimension, () -> {
            found.forEach((name, n) -> {
                note(String.format("spawn %s %s: listed=%s placement=%d rules=%d fits=%d of %d tries", dimension, name, n[0] > 0, n[1], n[2], n[3], n[4]));
                ok(n[0] > 0, name + " is on the spawn list in " + dimension);
                ok(n[3] > 0, name + " found somewhere to spawn in " + dimension);
            });
        });
    }

    private static void spawnTries(List<String> names, Map<String, int[]> found) {
        ServerPlayer p = player();
        ServerLevel level = p.serverLevel();
        var random = level.random;
        for (String name : names) {
            EntityType<?> type = typeNamed(name);
            int[] n = found.computeIfAbsent(name, key -> new int[5]);
            for (int tries = 0; tries < 800; tries++) {
                int x = p.getBlockX() + random.nextInt(97) - 48;
                int z = p.getBlockZ() + random.nextInt(97) - 48;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int y = random.nextBoolean() ? top : level.getMinBuildHeight() + 1 + random.nextInt(Math.max(1, top - level.getMinBuildHeight()));
                BlockPos pos = new BlockPos(x, y, z);
                n[4]++;
                for (var category : net.minecraft.world.entity.MobCategory.values()) {
                    if (level.getBiome(pos).value().getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type)) {
                        n[0]++;
                    }
                }
                if (!net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
                    continue;
                }
                n[1]++;
                @SuppressWarnings("unchecked")
                EntityType<Mob> mobType = (EntityType<Mob>) type;
                if (!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(mobType, level, MobSpawnType.NATURAL, pos, random)) {
                    continue;
                }
                n[2]++;
                if (!level.noCollision(type.getSpawnAABB(x + 0.5, y, z + 0.5))) {
                    continue;
                }
                Mob mob = mobType.create(level);
                mob.moveTo(x + 0.5, y, z + 0.5, 0.0f, 0.0f);
                if (mob.checkSpawnRules(level, MobSpawnType.NATURAL) && mob.checkSpawnObstruction(level)) {
                    n[3]++;
                }
                mob.discard();
            }
        }
    }

    private static void spawnWatch(String dimension, int ticks, List<String> names) {
        once("spawn " + dimension, () -> {
            survival();
            ServerLevel level = server().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
            server().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, server());
            overworld().setDayTime(18000L);
            nextArena += 700;
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player(), level, nextArena, level.getLogicalHeight() - 30, 0);
            player().getAbilities().mayfly = true;
            player().getAbilities().flying = true;
            player().onUpdateAbilities();
        });
        pause(ticks);
        once("spawn count " + dimension, () -> {
            ServerPlayer p = player();
            ServerLevel level = p.serverLevel();
            Map<String, Integer> counts = new TreeMap<>();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(128.0, 400.0, 128.0))) {
                counts.merge(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath(), 1, Integer::sum);
            }
            note("spawns after " + ticks / 20 + "s at " + dimension + ": " + counts);
            note("  of those listed there: " + names.stream().filter(counts::containsKey).toList() + "; not seen yet: " + names.stream().filter(n -> !counts.containsKey(n)).toList());
            clearAround();
        });
    }

    // ---- quit, reopen, and see what came through

    private static java.util.UUID flagId;
    private static java.util.UUID teenId;
    private static java.util.UUID cowId;
    private static java.util.UUID trophyId;
    private static int flagRung;
    private static BlockPos reloadSpot;

    private static void saveAndReload() {
        once("before the reload", () -> {
            survival();
            clearAround();
            BlockPos at = newArena();
            reloadSpot = at;
            ServerLevel level = overworld();
            ServerPlayer p = player();
            TowerFlag flag = spawn(ModContent.TOWER_FLAG.get(), level, at.getX() + 40.5, ARENA_Y, at.getZ() + 0.5);
            var saved = new net.minecraft.nbt.CompoundTag();
            flag.addAdditionalSaveData(saved);
            saved.putInt("Next", 7);
            flag.readAdditionalSaveData(saved);
            flagId = flag.getUUID();
            flagRung = 7;
            PrincessTeen teen = spawn(ModContent.PRINCESS_TEEN.get(), level, at.getX() + 8.5, ARENA_Y, at.getZ() + 0.5);
            teen.tame(p);
            teenId = teen.getUUID();
            Cow cow = spawn(EntityType.COW, level, at.getX() + 4.5, ARENA_Y, at.getZ() + 4.5);
            cow.setPersistenceRequired();
            ShrinkBolt.shrink(cow);
            cowId = cow.getUUID();
            Trophy trophy = spawn(ModContent.TROPHY_ENTITY.get(), level, at.getX() + 4.5, ARENA_Y, at.getZ() - 4.5);
            trophy.setSubject("orespawn_dangerzone:dargon");
            trophyId = trophy.getUUID();
            DzConfig.SWARM_SIZE.set(4);
            overworld().setDayTime(18000L);
            note("swarm before reload: " + Swarms.start(p));
        });
        pause(60);
        once("quit", () -> {
            note("swarm active before quitting: " + Swarms.activeCount() + ", builders " + DzBuilder.activeCount());
            reloadRequested = true;
        });
        pause(40);
        once("after the reload", () -> {
            ServerLevel level = overworld();
            ServerPlayer p = player();
            ok(Swarms.activeCount() == 0, "the swarm tracker starts empty in the reopened world");
            ok(DzBuilder.activeCount() == 0, "nothing's being built");
            ok(level.getEntity(flagId) instanceof TowerFlag f && f.rung() >= flagRung, "the flag came through on rung " + (level.getEntity(flagId) instanceof TowerFlag g ? g.rung() : -1));
            ok(level.getEntity(teenId) instanceof PrincessTeen t && t.isOwnedBy(p), "the tamed Teen Princess came through, still the player's");
            ok(level.getEntity(cowId) instanceof Cow c && c.hasEffect(ModContent.SHRUNK) && Math.abs(c.getScale() - 0.25f) < 0.01f, "the shrunk cow stayed shrunk");
            ok(level.getEntity(trophyId) instanceof Trophy t && t.subject().equals("orespawn_dangerzone:dargon"), "the trophy still knows its creature");
            long swarmers = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(400.0), m -> m.getPersistentData().getBoolean("orespawn_dangerzone_swarm")).size();
            ok(swarmers == 0, "and the swarm didn't survive the restart: " + swarmers + " left");
            DzConfig.SWARM_SIZE.set(60);
            clearAround();
        });
    }

    // ---- the quick round for what's being worked on

    private static void focusScript() {
        once("set up", () -> {
            survival();
            overworld().setDayTime(6000L);
        });
        fight("bloodlust_ant", ModContent.BLOODLUST_ANT.get(), 10, 300, true);
        fight("baby_sitter", ModContent.BABY_SITTER.get(), 16, 400, true);
        fight("chaos_demon", ModContent.CHAOS_DEMON.get(), 12, 300, true);
        fight("martian with a ray gun", ModContent.MARTIAN.get(), 12, 300, true, mob -> martian(mob, true));
        advancementsFire();
        naturalSpawning();
        Dargon[] dargon = new Dargon[1];
        Vec3[] from = new Vec3[1];
        once("a Dargon to tame", () -> {
            survival();
            BlockPos at = newArena();
            dargon[0] = spawn(ModContent.DARGON.get(), overworld(), at.getX() + 6.5, ARENA_Y, at.getZ() + 0.5);
            dargon[0].setNoAi(false);
            holdNothing();
        });
        pause(20);
        once("tame him", () -> {
            ServerPlayer p = player();
            dargon[0].setHealth(dargon[0].getMaxHealth() * 0.2f);
            p.interactOn(dargon[0], InteractionHand.MAIN_HAND);
            ok(dargon[0].isTame() && p.getVehicle() == dargon[0], "tamed and riding");
        });
        pause(40);
        once("fly him", () -> {
            from[0] = dargon[0].position();
            holdKeys = true;
        });
        pause(120);
        once("flew", () -> {
            holdKeys = false;
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().options.keyUp.setDown(false);
                Minecraft.getInstance().options.keyJump.setDown(false);
            });
            double moved = dargon[0].position().distanceTo(from[0]);
            note(String.format("moved him %.1f blocks, client forward=%.2f, client in control=%s, rider %s", moved, clientForward, clientRiding, player().getVehicle() == dargon[0] ? "on" : "off"));
            ok(moved > 3.0, "a rider can steer him");
            player().stopRiding();
            dargon[0].discard();
        });
    }

    // ---- a build cut off by quitting

    private static BlockPos ritualPos;

    private static void interruptScript() {
        once("start the carve", () -> {
            survival();
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player(), level(DzPlaces.DRAGONS_LAIR), 0, 120, 0);
        });
        pause(60);
        once("light it", () -> {
            ServerPlayer p = player();
            ServerLevel lair = p.serverLevel();
            BlockPos torch = new BlockPos(0, 110, 0);
            lair.getChunk(torch);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    lair.setBlockAndUpdate(torch.offset(dx, -1, dz), ModContent.DRAGONS_EYE_STONE.get().defaultBlockState());
                }
            }
            p.teleportTo(lair, 3.5, 111.0, 3.5, 0.0f, 0.0f);
            lair.setBlockAndUpdate(torch, ModContent.EXTREME_TORCH.get().defaultBlockState());
        });
        until("it's going", 40, () -> DzBuilder.activeCount() > 0);
        pause(100);
        once("quit part way", () -> {
            ok(DzBuilder.activeCount() > 0, "the carve is still going when we quit");
            log("done");
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
        });
    }

    private static void resumeScript() {
        once("resume", () -> {
            ok(DzBuilder.activeCount() > 0, "the cut-off carve started again by itself");
            note("builders running on open: " + DzBuilder.activeCount());
            survival();
            danger.orespawn.dangerzone.item.DimensionStaffItem.jumpTo(player(), level(DzPlaces.DRAGONS_LAIR), 3, 111, 3);
        });
        until("resumed carve finishes", 2000, () -> DzBuilder.activeCount() == 0);
        pause(40);
        once("resume result", () -> {
            ServerLevel lair = level(DzPlaces.DRAGONS_LAIR);
            BlockPos torch = new BlockPos(0, 110, 0);
            AABB box = new AABB(torch).inflate(120.0, 80.0, 260.0);
            ok(lair.getBlockState(torch.offset(30, 20, 50)).isAir(), "the void is open");
            ok(lair.getEntitiesOfClass(Dargon.class, box).size() == 1, "there's exactly one Dargon: " + lair.getEntitiesOfClass(Dargon.class, box).size());
            ok(lair.getEntitiesOfClass(ElementDragon.class, box).size() >= 6, "and his dragons: " + lair.getEntitiesOfClass(ElementDragon.class, box).size());
        });
    }

    // ---- a world from before these dimensions existed

    private static void oldWorldScript() {
        once("old world", () -> {
            note("world " + worldName);
            for (ResourceKey<Level> key : List.of(DzPlaces.KRAKEN_REALM, DzPlaces.DRAGONS_LAIR, DzPlaces.DOOM)) {
                ok(level(key) != null, key.location() + " exists in a world made before it was added");
            }
        });
        for (ResourceKey<Level> key : List.of(DzPlaces.KRAKEN_REALM, DzPlaces.DRAGONS_LAIR, DzPlaces.DOOM)) {
            once("visit " + key.location(), () -> {
                if (level(key) != null) {
                    BlockPos stand = danger.orespawn.dangerzone.item.DimensionStaffItem.jump(player(), level(key));
                    ok(player().level().dimension() == key && !level(key).getBlockState(stand.below()).isAir(), "and can be visited, at " + stand.toShortString());
                }
            });
            pause(60);
        }
    }
}
