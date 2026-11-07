package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BabyNope;
import danger.orespawn.dangerzone.entity.Martian;
import danger.orespawn.dangerzone.entity.Nope;
import danger.orespawn.dangerzone.entity.Needlenose;
import danger.orespawn.dangerzone.entity.PrincessGrowth;
import danger.orespawn.dangerzone.entity.PrincessTeen;
import danger.orespawn.dangerzone.entity.PrincessYoungAdult;
import danger.orespawn.dangerzone.entity.Swarms;
import danger.orespawn.dangerzone.entity.TowerFlag;
import danger.orespawn.dangerzone.entity.Trophy;
import danger.orespawn.dangerzone.entity.Ufo;
import danger.orespawn.dangerzone.entity.UfoCarrier;
import danger.orespawn.dangerzone.entity.UfoFighter;
import danger.orespawn.dangerzone.entity.Volcano;
import danger.orespawn.dangerzone.item.TrophyItem;
import danger.orespawn.entity.Acid;
import danger.orespawn.entity.LaserBall;
import danger.orespawn.entity.ThePrinceAdult;
import danger.orespawn.entity.ThePrinceTeen;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.List;

@GameTestHolder("orespawn_dangerzone_tests")
@PrefixGameTestTemplate(false)
public final class MilestoneTwoTests {
    private MilestoneTwoTests() {
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static AABB around(GameTestHelper helper, double radius) {
        return new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(radius);
    }

    private static <T extends net.minecraft.world.entity.Entity> List<T> near(GameTestHelper helper, Class<T> type, double radius) {
        return helper.getLevel().getEntitiesOfClass(type, around(helper, radius));
    }

    private static void setInt(Object target, Class<?> owner, String name, int value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    // ------------------------------------------------------------------ the flag

    @GameTest(template = "empty8")
    public static void flagLadderGoesWeakestToStrongest(GameTestHelper helper) {
        List<EntityType<?>> ladder = TowerFlag.ladder();
        check(helper, ladder.size() >= 40, "only " + ladder.size() + " species on the ladder");
        for (int i = 1; i < ladder.size(); i++) {
            check(helper, TowerFlag.hardness(ladder.get(i - 1)) <= TowerFlag.hardness(ladder.get(i)), "out of order at " + i);
        }
        check(helper, ladder.contains(ModContent.ANT_QUEEN.get()), "the Queen should be on the ladder");
        check(helper, TowerFlag.waveSize(1.0) == 12 && TowerFlag.waveSize(1000.0) == 4, "wave sizes should step from 12 down to 4");
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 200)
    public static void flagCallsAWaveThatGoesForIt(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        TowerFlag flag = helper.spawn(ModContent.TOWER_FLAG.get(), new BlockPos(4, 1, 4));
        EntityType<?> first = TowerFlag.ladder().get(0);
        flag.nextWave(level);
        List<Mob> wave = level.getEntitiesOfClass(Mob.class, flag.getBoundingBox().inflate(48.0), mob -> mob.getType() == first);
        // At least: the neighbouring Ant Queen test can call the same species nearby.
        check(helper, wave.size() >= TowerFlag.waveSize(TowerFlag.hardness(first)), "expected a full wave of " + first + ", found " + wave.size());
        check(helper, wave.stream().filter(mob -> mob.getTarget() == flag).count() >= TowerFlag.waveSize(TowerFlag.hardness(first)), "the wave should go for the flag: " + wave.stream().filter(mob -> mob.getTarget() == flag).count() + " of " + wave.size() + " of " + first);
        check(helper, flag.rung() == 1, "should have moved up a rung");
        wave.forEach(Mob::discard);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void flagShrugsOffPlayersUntilItsOverThenPaysOnce(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        TowerFlag flag = helper.spawn(ModContent.TOWER_FLAG.get(), new BlockPos(4, 1, 4));
        check(helper, !flag.hurt(level.damageSources().playerAttack(player), 10.0f), "a player can't hurt it during the event");
        check(helper, flag.getHealth() == flag.getMaxHealth(), "health shouldn't have moved");

        CompoundTag saved = new CompoundTag();
        flag.addAdditionalSaveData(saved);
        saved.putInt("Next", TowerFlag.ladder().size());
        flag.readAdditionalSaveData(saved);
        flag.nextWave(level);
        check(helper, flag.finished(), "past the last rung the event is over");
        List<ItemEntity> paid = near(helper, ItemEntity.class, 16.0);
        check(helper, paid.stream().anyMatch(e -> e.getItem().is(ModContent.TOWER_FLAG_ITEM.get())), "the flag comes back");
        check(helper, paid.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND)), "there should be a payout");
        check(helper, flag.hurt(level.damageSources().playerAttack(player), 10.0f), "a player can hurt it once it's over");
        helper.succeed();
    }

    /** The test server only ticks chunks near the test, so this starts them close; the photo run measures a whole wave. */
    @GameTest(template = "empty8", timeoutTicks = 400)
    public static void aCreatureThatWontChaseStillHurtsTheFlag(GameTestHelper helper) {
        floor(helper);
        TowerFlag flag = helper.spawn(ModContent.TOWER_FLAG.get(), new BlockPos(4, 1, 4));
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "gamma_metroid"));
        Mob mob = (Mob) helper.spawn(type, new BlockPos(1, 1, 1));
        flag.addAttacker(mob);
        helper.succeedWhen(() -> helper.assertTrue(flag.getHealth() < flag.getMaxHealth(), "the flag hasn't been hurt yet"));
    }

    @GameTest(template = "empty8", timeoutTicks = 120, batch = "griefing")
    public static void flagClearsAroundItButLeavesContainers(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.CHEST);
        for (int y = 1; y < 8; y++) {
            helper.setBlock(new BlockPos(3, y, 4), Blocks.STONE);
        }
        helper.spawn(ModContent.TOWER_FLAG.get(), new BlockPos(4, 1, 4));
        helper.runAfterDelay(100, () -> {
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(5, 1, 4));
            int left = 0;
            for (int y = 1; y < 8; y++) {
                left += helper.getBlockState(new BlockPos(3, y, 4)).is(Blocks.STONE) ? 1 : 0;
            }
            check(helper, left < 7, "none of the stone next to the flag was cleared");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ trophies

    @GameTest(template = "empty8")
    public static void trophyRemembersItsCreature(GameTestHelper helper) {
        ItemStack stack = TrophyItem.of("orespawn_dangerzone:nope");
        check(helper, TrophyItem.subject(stack).equals("orespawn_dangerzone:nope"), "should remember the creature");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void trophyFallsOffWhenItsBlockGoes(GameTestHelper helper) {
        floor(helper);
        Trophy trophy = helper.spawn(ModContent.TROPHY_ENTITY.get(), new BlockPos(4, 1, 4));
        trophy.setSubject("orespawn_dangerzone:needlenose");
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.AIR);
        helper.runAfterDelay(5, () -> {
            check(helper, trophy.isRemoved(), "it should have popped off");
            List<ItemEntity> drops = near(helper, ItemEntity.class, 6.0);
            check(helper, drops.stream().anyMatch(e -> TrophyItem.subject(e.getItem()).equals("orespawn_dangerzone:needlenose")), "it should come back as its own item");
            helper.succeed();
        });
    }

    @GameTest(template = "empty8", timeoutTicks = 60)
    public static void creaturesDropTheirTrophyAndHeadsAndShrunkDont(GameTestHelper helper) {
        floor(helper);
        double before = DzConfig.TROPHY_CHANCE.get();
        DzConfig.TROPHY_CHANCE.set(1.0);
        Needlenose mob = helper.spawn(ModContent.NEEDLENOSE.get(), new BlockPos(2, 1, 2));
        mob.hurt(helper.getLevel().damageSources().genericKill(), 1000.0f);
        Needlenose shrunk = helper.spawn(ModContent.NEEDLENOSE.get(), new BlockPos(6, 1, 6));
        danger.orespawn.dangerzone.entity.ShrinkBolt.shrink(shrunk);
        shrunk.hurt(helper.getLevel().damageSources().genericKill(), 1000.0f);
        DzConfig.TROPHY_CHANCE.set(before);
        long trophies = near(helper, ItemEntity.class, 12.0).stream().filter(e -> e.getItem().is(ModContent.TROPHY.get())).count();
        check(helper, trophies == 1, "one trophy from the normal one and none from the shrunk one, found " + trophies);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Nope

    @GameTest(template = "empty8")
    public static void nopeSpitsAcidAndCapsItsBabies(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        Nope nope = helper.spawn(ModContent.NOPE.get(), new BlockPos(1, 1, 1));
        Cow victim = helper.spawn(EntityType.COW, new BlockPos(6, 1, 6));
        nope.spit(level, victim);
        check(helper, !near(helper, Acid.class, 16.0).isEmpty(), "no acid in the air");
        near(helper, BabyNope.class, 32.0).forEach(BabyNope::discard);
        int limit = DzConfig.NOPE_MAX_BABIES.get();
        int called = 0;
        for (int i = 0; i < 400; i++) {
            called += nope.callBaby(level, victim) ? 1 : 0;
        }
        check(helper, called > 0 && called <= limit, "called " + called + " with a limit of " + limit);
        BabyNope baby = near(helper, BabyNope.class, 32.0).get(0);
        check(helper, baby.getTarget() == victim, "a baby should go for the victim");
        nope.setTarget(baby);
        check(helper, nope.getTarget() == null, "a Nope shouldn't turn on its babies");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the Princess

    @GameTest(template = "empty8")
    public static void princessGrowsAndShrinksThroughHerOwnStages(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        TamableAnimal baby = (TamableAnimal) PrincessGrowth.baby().create(helper.getLevel());
        baby.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        baby.tame(player);
        helper.getLevel().addFreshEntity(baby);

        ItemStack block = new ItemStack(Blocks.DIAMOND_BLOCK, 4);
        PrincessGrowth.become(baby, ModContent.PRINCESS_TEEN.get(), player, block);
        PrincessTeen teen = near(helper, PrincessTeen.class, 8.0).stream().findFirst().orElse(null);
        check(helper, teen != null && baby.isRemoved(), "the baby should be a Teen now");
        check(helper, teen.isOwnedBy(player), "she should still be the player's");
        check(helper, block.getCount() == 3, "that should have cost one diamond block");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.DIAMOND_BLOCK));
        teen.mobInteract(player, InteractionHand.MAIN_HAND);
        PrincessYoungAdult adult = near(helper, PrincessYoungAdult.class, 8.0).stream().findFirst().orElse(null);
        check(helper, adult != null && teen.isRemoved(), "a diamond block should make the Teen a Young Adult");
        check(helper, near(helper, ThePrinceAdult.class, 8.0).stream().allMatch(e -> e instanceof PrincessYoungAdult), "she shouldn't have become a Prince");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        adult.mobInteract(player, InteractionHand.MAIN_HAND);
        PrincessTeen back = near(helper, PrincessTeen.class, 8.0).stream().filter(e -> !e.isRemoved()).findFirst().orElse(null);
        check(helper, back != null && adult.isRemoved(), "a diamond should take her back to a Teen");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        back.mobInteract(player, InteractionHand.MAIN_HAND);
        check(helper, back.isRemoved(), "a diamond should take the Teen back to a baby");
        check(helper, helper.getLevel().getEntitiesOfClass(TamableAnimal.class, around(helper, 8.0), e -> e.getType() == PrincessGrowth.baby() && !e.isRemoved()).size() == 1, "she should be a baby Princess again");
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 40)
    public static void teenPrincessNeverGrowsIntoAPrince(GameTestHelper helper) throws ReflectiveOperationException {
        floor(helper);
        PrincessTeen teen = helper.spawn(ModContent.PRINCESS_TEEN.get(), new BlockPos(4, 1, 4));
        // The port's own condition for a Teen to grow up by itself.
        setInt(teen, ThePrinceTeen.class, "killCount", 1000);
        setInt(teen, ThePrinceTeen.class, "dayCount", 1000);
        helper.runAfterDelay(10, () -> {
            check(helper, !teen.isRemoved(), "she shouldn't have changed");
            check(helper, near(helper, ThePrinceAdult.class, 16.0).isEmpty(), "a Prince appeared");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ swarms

    @GameTest(template = "empty8", timeoutTicks = 60)
    public static void swarmCrossesAndCanBeCalled(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        int size = DzConfig.SWARM_SIZE.get();
        DzConfig.SWARM_SIZE.set(4);
        int came = Swarms.start(player);
        DzConfig.SWARM_SIZE.set(size);
        // The test world's hour may have none: then the choice says so too.
        if (came == 0) {
            check(helper, Swarms.choice(level, level.random) == null, "the table has a choice, yet nothing came");
            helper.succeed();
            return;
        }
        check(helper, came <= 4, "came " + came + " with a size of 4");
        check(helper, Swarms.activeCount() >= came, "the tracker should know them");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the volcano

    @GameTest(template = "empty8", timeoutTicks = 40)
    public static void volcanoThrowsUpToItsLimit(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        Volcano volcano = helper.spawn(ModContent.VOLCANO.get(), new BlockPos(4, 1, 4));
        int thrown = 0;
        for (int i = 0; i < 300; i++) {
            thrown += volcano.spoutFrom(level, 2);
        }
        check(helper, thrown > 0, "nothing was thrown");
        long flying = near(helper, FallingBlockEntity.class, 96.0).size();
        check(helper, flying <= DzConfig.VOLCANO_MAX_FLYING.get() + 6, flying + " in the air");
        near(helper, FallingBlockEntity.class, 96.0).forEach(FallingBlockEntity::discard);
        near(helper, net.minecraft.world.entity.item.PrimedTnt.class, 96.0).forEach(e -> e.discard());
        volcano.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 100, batch = "griefing2")
    public static void volcanoIsHarmlessWithoutMobGriefing(GameTestHelper helper) {
        floor(helper);
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        helper.spawn(ModContent.VOLCANO.get(), new BlockPos(4, 1, 4));
        helper.runAfterDelay(60, () -> {
            boolean none = near(helper, FallingBlockEntity.class, 96.0).isEmpty();
            rule.set(true, helper.getLevel().getServer());
            near(helper, Volcano.class, 8.0).forEach(Volcano::discard);
            check(helper, none, "it threw blocks with mobGriefing off");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the UFOs

    @GameTest(template = "empty8")
    public static void ufoStopsLaunchingAtItsLimit(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        Ufo ufo = helper.spawn(ModContent.UFO.get(), new BlockPos(4, 20, 4));
        int limit = DzConfig.UFO_MAX_CRAFT.get();
        int launched = 0;
        for (int i = 0; i < 100; i++) {
            launched += ufo.launch(level) ? 1 : 0;
        }
        check(helper, launched == limit, "launched " + launched + " with a limit of " + limit);
        near(helper, UfoFighter.class, 64.0).forEach(UfoFighter::discard);
        near(helper, UfoCarrier.class, 64.0).forEach(UfoCarrier::discard);
        ufo.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void carrierLetsOutMartiansWhenItLands(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        UfoCarrier carrier = helper.spawn(ModContent.UFO_CARRIER.get(), new BlockPos(4, 1, 4));
        int out = carrier.land(level);
        check(helper, out > 0 && out <= 10, "let out " + out);
        check(helper, carrier.isRemoved(), "the ship should be gone");
        List<Martian> martians = near(helper, Martian.class, 16.0);
        check(helper, martians.size() == out, "found " + martians.size() + " of " + out);
        martians.get(0).setTarget(martians.get(martians.size() - 1));
        check(helper, martians.get(0).getTarget() == null, "Martians shouldn't fight each other");
        martians.forEach(Martian::discard);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void fighterFiresLasers(GameTestHelper helper) {
        floor(helper);
        UfoFighter fighter = helper.spawn(ModContent.UFO_FIGHTER.get(), new BlockPos(4, 3, 4));
        fighter.fire(helper.getLevel(), new Vec3(1.0, -0.5, 0.0));
        check(helper, !near(helper, LaserBall.class, 16.0).isEmpty(), "no laser in the air");
        fighter.discard();
        helper.succeed();
    }
}
