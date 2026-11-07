package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BloodlustAnt;
import danger.orespawn.dangerzone.entity.NastysaurusPrime;
import danger.orespawn.dangerzone.entity.Needlenose;
import danger.orespawn.dangerzone.entity.NurseBug;
import danger.orespawn.dangerzone.entity.ShrinkBolt;
import danger.orespawn.dangerzone.entity.Summons;
import danger.orespawn.dangerzone.item.DimensionStaffItem;
import danger.orespawn.dangerzone.item.DragonsEyeSwordItem;
import danger.orespawn.dangerzone.recipe.DuctTapeRepairRecipe;
import danger.orespawn.entity.Nastysaurus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("orespawn_dangerzone_tests")
@PrefixGameTestTemplate(false)
public final class DangerZoneTests {
    private DangerZoneTests() {
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

    @GameTest(template = "empty8")
    public static void primeKeepsDangerZonesProportionsOverThePortsNastysaurus(GameTestHelper helper) {
        floor(helper);
        NastysaurusPrime prime = helper.spawn(ModContent.NASTYSAURUS_PRIME.get(), new BlockPos(4, 1, 4));
        Nastysaurus plain = helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("orespawn", "nastysaurus")) instanceof EntityType<?> t
                ? (EntityType<Nastysaurus>) t : null, new BlockPos(1, 1, 1));
        check(helper, prime.getMaxHealth() == plain.getMaxHealth() * 2.0f, "Prime should have twice a Nastysaurus's health");
        double bite = prime.getAttributeValue(Attributes.ATTACK_DAMAGE) / plain.getAttributeValue(Attributes.ATTACK_DAMAGE);
        check(helper, Math.abs(bite - 1.7) < 0.05, "Prime's bite should be about 1.7 times a Nastysaurus's, was " + bite);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void primeCallsNeedlenosesUpToThePackLimit(GameTestHelper helper) {
        floor(helper);
        NastysaurusPrime prime = helper.spawn(ModContent.NASTYSAURUS_PRIME.get(), new BlockPos(1, 1, 1));
        Cow victim = helper.spawn(EntityType.COW, new BlockPos(6, 1, 6));
        ServerLevel level = helper.getLevel();
        int limit = DzConfig.PRIME_MAX_PACK.get();
        int called = 0;
        for (int i = 0; i < 400; i++) {
            called += prime.callPack(level, victim) ? 1 : 0;
        }
        check(helper, called > 0, "no Needlenose came");
        check(helper, called <= limit, "called " + called + " past the limit of " + limit);
        Needlenose first = level.getEntitiesOfClass(Needlenose.class, prime.getBoundingBox().inflate(32.0)).get(0);
        check(helper, first.getTarget() == victim, "a called Needlenose should go for the victim");
        prime.setTarget(first);
        check(helper, prime.getTarget() == null, "a Prime shouldn't turn on its own pack");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void queensBroodStopsAtItsLimitAndBloodlustTakesHerTarget(GameTestHelper helper) {
        floor(helper);
        AntQueen queen = helper.spawn(ModContent.ANT_QUEEN.get(), new BlockPos(4, 1, 4));
        Cow victim = helper.spawn(EntityType.COW, new BlockPos(7, 1, 7));
        ServerLevel level = helper.getLevel();
        BlockPos center = queen.blockPosition();
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        int limit = DzConfig.QUEEN_MAX_BROOD.get();
        int came = 0;
        for (int i = 0; i < 400; i++) {
            came += queen.callBrood(level, victim);
        }
        check(helper, came > 0, "no brood came");
        check(helper, came <= limit, "called " + came + " past the limit of " + limit);
        List<BloodlustAnt> soldiers = level.getEntitiesOfClass(BloodlustAnt.class, queen.getBoundingBox().inflate(32.0));
        check(helper, !soldiers.isEmpty(), "no Bloodlust Ant among the brood");
        check(helper, soldiers.get(0).getTarget() == victim, "a Bloodlust Ant should go for the victim");
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 300)
    public static void nurseBugFliesToAHurtPlayerAndHeals(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1.0, 1.5)));
        player.setHealth(4.0f);
        NurseBug bug = helper.spawn(ModContent.NURSE_BUG.get(), new BlockPos(5, 2, 5));
        helper.succeedWhen(() -> {
            check(helper, bug.isAlive(), "the Nurse Bug died");
            helper.assertTrue(player.getHealth() > 4.0f, "the player wasn't healed yet");
        });
    }

    @GameTest(template = "empty8")
    public static void nurseBugIgnoresMonstersAndHealthyPlayers(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        check(helper, !NurseBug.needsNurse(player), "a healthy player needs no nurse");
        player.setHealth(2.0f);
        check(helper, NurseBug.needsNurse(player), "a hurt player needs the nurse");
        Needlenose monster = helper.spawn(ModContent.NEEDLENOSE.get(), new BlockPos(3, 1, 3));
        monster.setHealth(1.0f);
        check(helper, !NurseBug.needsNurse(monster), "a hurt monster gets no nurse");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void shrinkRayShrinksCreaturesButNotBosses(GameTestHelper helper) {
        floor(helper);
        Needlenose small = helper.spawn(ModContent.NEEDLENOSE.get(), new BlockPos(2, 1, 2));
        double bite = small.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double width = small.getBbWidth();
        check(helper, ShrinkBolt.shrink(small), "a Needlenose should shrink");
        check(helper, Math.abs(small.getScale() - 0.25f) < 0.01f, "a quarter the size, was " + small.getScale());
        check(helper, Math.abs(small.getAttributeValue(Attributes.ATTACK_DAMAGE) - bite / 16.0) < 0.01, "a sixteenth of the bite");
        check(helper, small.getEffect(ModContent.SHRUNK).isInfiniteDuration(), "creatures stay shrunk");

        AntQueen queen = helper.spawn(ModContent.ANT_QUEEN.get(), new BlockPos(4, 1, 4));
        check(helper, !ShrinkBolt.shrink(queen), "the Queen is immune");
        check(helper, queen.getScale() == 1.0f, "the Queen should keep her size");
        // Minecraft refreshes the hitbox on the entity's next tick.
        helper.runAfterDelay(3, () -> {
            check(helper, Math.abs(small.getBbWidth() - width * 0.25) < 0.01, "hitbox should shrink with it, was " + small.getBbWidth());
            helper.succeed();
        });
    }

    @GameTest(template = "empty8")
    public static void shrinkRayWearsOffPlayers(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        check(helper, ShrinkBolt.shrink(player), "a player can be shrunk");
        check(helper, !player.getEffect(ModContent.SHRUNK).isInfiniteDuration(), "a player grows back");
        helper.succeed();
    }

    private static ItemStack tape() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("orespawn", "duct_tape")));
    }

    @GameTest(template = "empty8")
    public static void ductTapeFullyRepairsAndKeepsTheItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DuctTapeRepairRecipe recipe = new DuctTapeRepairRecipe(CraftingBookCategory.MISC);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(1000);
        sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Old Faithful"));
        CraftingInput ok = CraftingInput.of(2, 1, List.of(tape(), sword));
        check(helper, recipe.matches(ok, level), "tape and a damaged sword should match");
        ItemStack out = recipe.assemble(ok, level.registryAccess());
        check(helper, out.is(Items.DIAMOND_SWORD) && out.getDamageValue() == 0, "should be fully repaired, damage " + out.getDamageValue());
        check(helper, out.getHoverName().getString().equals("Old Faithful"), "should keep what's on the item");

        check(helper, !recipe.matches(CraftingInput.of(2, 1, List.of(tape(), new ItemStack(Items.DIAMOND_SWORD))), level), "an undamaged sword shouldn't match");
        check(helper, !recipe.matches(CraftingInput.of(2, 1, List.of(new ItemStack(Items.STICK), sword)), level), "no tape shouldn't match");
        check(helper, !recipe.matches(CraftingInput.of(2, 1, List.of(tape(), new ItemStack(Items.STONE, 2))), level), "a stack shouldn't match");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void dimensionStaffRingsThroughEveryDimensionBothWays(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ResourceKey<Level> home = Level.OVERWORLD;
        ServerLevel next = DimensionStaffItem.neighbour(server, home, true);
        check(helper, next != null && next.dimension() != home, "there should be a next dimension");
        ServerLevel back = DimensionStaffItem.neighbour(server, next.dimension(), false);
        check(helper, back != null && back.dimension() == home, "going back should return home");
        int steps = 0;
        ResourceKey<Level> at = home;
        do {
            at = DimensionStaffItem.neighbour(server, at, true).dimension();
            steps++;
        } while (at != home && steps < 100);
        check(helper, steps == server.levelKeys().size(), "the ring should visit each of " + server.levelKeys().size() + ", took " + steps);
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 400)
    public static void dimensionStaffLandsSafelyInEveryDimensionOfThisServer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerPlayer player = MockPlayers.inLevel(helper);
        // The test server builds only the vanilla dimensions; the photo run checks OreSpawn's in a real world.
        for (ResourceKey<Level> key : List.of(Level.NETHER, Level.END)) {
            String name = key.location().toString();
            ServerLevel target = server.getLevel(key);
            check(helper, target != null, "no dimension " + name);
            BlockPos stand = DimensionStaffItem.jump(player, target);
            check(helper, player.level() == target, "should have arrived in " + name);
            check(helper, target.getBlockState(stand.below()).isSolid(), "should stand on something solid in " + name + " at " + stand);
            check(helper, target.getBlockState(stand).isAir() || !target.getBlockState(stand).isSolid(), "shouldn't be inside a block in " + name);
        }
        // Home again and gone, so the mock player holds nothing open in the other dimensions at shutdown.
        DimensionStaffItem.jump(player, helper.getLevel());
        server.getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void dragonsEyeSwordBlinksToSolidGroundOnly(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 1.0, 4.5)));
        Cow target = helper.spawn(EntityType.COW, new BlockPos(5, 1, 4));
        BlockPos before = player.blockPosition();
        boolean moved = false;
        for (int i = 0; i < 5; i++) {
            moved |= DragonsEyeSwordItem.blink(player, target) && !player.blockPosition().equals(before);
        }
        check(helper, moved, "the sword should have blinked");
        check(helper, helper.getLevel().getBlockState(player.blockPosition().below()).isSolid(), "should land on solid ground");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void spawnListsReachTheBiomes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Biome chaos = biomes.get(ResourceLocation.fromNamespaceAndPath("orespawn", "chaos_biome"));
        Biome mining = biomes.get(ResourceLocation.fromNamespaceAndPath("orespawn", "mining_biome"));
        Holder<Biome> chaosHolder = biomes.wrapAsHolder(chaos);
        Holder<Biome> miningHolder = biomes.wrapAsHolder(mining);
        check(helper, spawns(chaosHolder, ModContent.ANT_QUEEN.get()), "the Queen should spawn in Chaos");
        check(helper, spawns(miningHolder, ModContent.NEEDLENOSE.get()), "Needlenoses should spawn in Mining");
        check(helper, spawns(miningHolder, ModContent.NASTYSAURUS_PRIME.get()), "the Prime should spawn in Mining");
        helper.succeed();
    }

    private static boolean spawns(Holder<Biome> biome, EntityType<?> type) {
        return biome.value().getMobSettings().getMobs(type.getCategory()).unwrap().stream()
                .map(data -> ((MobSpawnSettings.SpawnerData) data).type).anyMatch(type::equals);
    }

    @GameTest(template = "empty8")
    public static void dataLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        long tasks = server.getAdvancements().getAllAdvancements().stream()
                .filter(a -> a.id().getNamespace().equals(OreSpawnDangerZone.MOD_ID)).count();
        check(helper, tasks >= 140, "only " + tasks + " advancements loaded");
        for (String recipe : List.of("dimension_staff", "dragons_eye_sword", "shrink_ray", "duct_tape_repair")) {
            check(helper, server.getRecipeManager().byKey(OreSpawnDangerZone.id(recipe)).isPresent(), "missing recipe " + recipe);
        }
        for (String table : List.of("nastysaurus_prime", "needlenose", "nurse_bug", "ant_queen", "bloodlust_ant")) {
            var key = ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("entities/" + table));
            check(helper, server.reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "missing loot table " + table);
        }
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void summonsRefusesToBuryAHelper(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                for (int y = 0; y < 8; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos middle = helper.absolutePos(new BlockPos(4, 4, 4));
        check(helper, Summons.footing(helper.getLevel(), middle.getX(), middle.getY(), middle.getZ(), 1) == null, "there's nowhere to stand in solid rock");
        helper.succeed();
    }
}
