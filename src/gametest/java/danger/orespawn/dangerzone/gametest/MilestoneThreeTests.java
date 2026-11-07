package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.BabySitter;
import danger.orespawn.dangerzone.entity.ChaosDemon;
import danger.orespawn.dangerzone.entity.Dargon;
import danger.orespawn.dangerzone.entity.DelayedEvent;
import danger.orespawn.dangerzone.entity.DoomWorm;
import danger.orespawn.dangerzone.entity.ElementDragon;
import danger.orespawn.dangerzone.entity.ElementShot;
import danger.orespawn.dangerzone.entity.KrakenKing;
import danger.orespawn.dangerzone.entity.PrincessYoungAdult;
import danger.orespawn.dangerzone.entity.PurpleFireball;
import danger.orespawn.dangerzone.world.DzBlocks;
import danger.orespawn.dangerzone.world.DzBuilder;
import danger.orespawn.dangerzone.world.DzDungeons;
import danger.orespawn.dangerzone.world.DzPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("orespawn_dangerzone_tests")
@PrefixGameTestTemplate(false)
public final class MilestoneThreeTests {
    private MilestoneThreeTests() {
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

    private static <T extends net.minecraft.world.entity.Entity> List<T> near(GameTestHelper helper, Class<T> type, double radius) {
        return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(radius));
    }

    // ------------------------------------------------------------------ the chain

    @GameTest(template = "empty8")
    public static void chaosDemonTeleportsToWhoeverHitsIt(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        ChaosDemon demon = helper.spawn(ModContent.CHAOS_DEMON.get(), new BlockPos(6, 1, 6));
        demon.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        check(helper, demon.distanceTo(player) < 15.0, "it should be within reach of its attacker, was " + demon.distanceTo(player));
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void theChainGoesDemonThenSitterThenWarp(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        ChaosDemon demon = helper.spawn(ModContent.CHAOS_DEMON.get(), new BlockPos(5, 1, 5));
        demon.hurt(level.damageSources().playerAttack(player), 100000.0f);
        check(helper, demon.isDeadOrDying(), "the demon should be dead");
        DelayedEvent first = near(helper, DelayedEvent.class, 12.0).stream().findFirst().orElse(null);
        check(helper, first != null, "nothing was left behind by the demon");
        first.fire(level);
        first.discard();
        BabySitter sitter = near(helper, BabySitter.class, 24.0).stream().findFirst().orElse(null);
        check(helper, sitter != null, "the Baby Sitter should have come");
        check(helper, sitter.getTarget() == player, "she should come for the killer");
        sitter.hurt(level.damageSources().playerAttack(player), 100000.0f);
        check(helper, sitter.isDeadOrDying(), "the Baby Sitter should be dead");
        check(helper, !near(helper, DelayedEvent.class, 24.0).isEmpty(), "she should have left the warp behind");
        near(helper, DelayedEvent.class, 24.0).forEach(DelayedEvent::discard);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void krakenKingCallsThePrincessOnceAndDropsTheStaff(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        KrakenKing king = helper.spawn(ModContent.KRAKEN_KING.get(), new BlockPos(4, 3, 4));
        check(helper, king.getMaxHealth() == 8000.0f, "he should have 8000 health, has " + king.getMaxHealth());
        check(helper, !king.hurt(level.damageSources().explosion(null, null), 10.0f), "explosions shouldn't hurt him");
        check(helper, near(helper, PrincessYoungAdult.class, 32.0).isEmpty(), "she shouldn't come yet");
        king.hurt(level.damageSources().playerAttack(player), 10.0f);
        check(helper, near(helper, PrincessYoungAdult.class, 32.0).size() == 1, "the first hit should bring her");
        king.invulnerableTime = 0;
        king.hurt(level.damageSources().playerAttack(player), 10.0f);
        check(helper, near(helper, PrincessYoungAdult.class, 32.0).size() == 1, "only the first hit brings her");
        king.hurt(level.damageSources().genericKill(), 100000.0f);
        check(helper, near(helper, ItemEntity.class, 40.0).stream().anyMatch(e -> e.getItem().is(ModContent.DIMENSION_STAFF.get())), "he should drop the Dimension Staff");
        near(helper, PrincessYoungAdult.class, 32.0).forEach(PrincessYoungAdult::discard);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aDyingKingCallsNoMoreKings(GameTestHelper helper) throws ReflectiveOperationException {
        KrakenKing king = helper.spawn(ModContent.KRAKEN_KING.get(), new BlockPos(4, 3, 4));
        java.lang.reflect.Field field = danger.orespawn.entity.Kraken.class.getDeclaredField("callReinforcements");
        field.setAccessible(true);
        check(helper, field.getInt(king) != 0, "he should have his reinforcements already spent");
        king.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void castlesAreOnAGridIncludingNegativeCoordinates(GameTestHelper helper) {
        check(helper, DzPlaces.isCastleChunk(29, 29), "chunk 29,29 has a castle");
        check(helper, DzPlaces.isCastleChunk(29 - 128, 29 - 256), "negative coordinates have them too");
        check(helper, !DzPlaces.isCastleChunk(30, 29), "not next door");
        ChunkPos near = DzPlaces.nearestCastle(-5000.0, 7000.0);
        check(helper, DzPlaces.isCastleChunk(near.x, near.z), "the nearest castle should be a castle chunk");
        check(helper, Math.abs(near.getMinBlockX() + 5000) <= 1024 + 16 && Math.abs(near.getMinBlockZ() - 7000) <= 1024 + 16, "it should be near: " + near);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void theTranslatedBuildersRun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DzBuilder castle = new DzBuilder(level);
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        DzDungeons.makeKrakenKingCastle(castle, at.getX(), at.getY(), at.getZ());
        check(helper, castle.pending() > 10000, "the castle only asked for " + castle.pending() + " blocks");
        DzBuilder lair = new DzBuilder(level);
        DzDungeons.makeDargonsLair(lair, at.getX(), at.getY(), at.getZ());
        check(helper, lair.pending() > 4000000, "the Lair's carve should be over four million blocks: " + lair.pending());
        helper.succeed();
    }

    // ------------------------------------------------------------------ the Lair

    @GameTest(template = "empty8")
    public static void extremeTorchNeedsAFullPad(GameTestHelper helper) {
        floor(helper);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(new BlockPos(4 + dx, 0, 4 + dz), ModContent.DRAGONS_EYE_STONE.get());
            }
        }
        BlockPos torch = helper.absolutePos(new BlockPos(4, 1, 4));
        check(helper, DzBlocks.ExtremeTorch.padReady(helper.getLevel(), torch), "nine stones should do");
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        check(helper, !DzBlocks.ExtremeTorch.padReady(helper.getLevel(), torch), "eight shouldn't");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void dargonIsTamedOnlyWhenWeakAndByAnEmptyHand(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        Dargon dargon = helper.spawn(ModContent.DARGON.get(), new BlockPos(4, 1, 4));
        dargon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(helper, !dargon.isTame(), "at full health he shouldn't tame");
        dargon.setHealth(dargon.getMaxHealth() * 0.2f);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        dargon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(helper, !dargon.isTame(), "with something in hand he shouldn't tame");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        dargon.mobInteract(player, InteractionHand.MAIN_HAND);
        check(helper, dargon.isTame() && dargon.isOwnedBy(player), "weak and empty-handed should tame him");
        check(helper, player.getVehicle() == dargon, "he should carry his tamer");
        player.stopRiding();
        dargon.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void dargonsVolleyIsItsNumberAndFireDoesntHurtHim(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        Dargon dargon = helper.spawn(ModContent.DARGON.get(), new BlockPos(4, 1, 4));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        int shots = dargon.volley(level, cow);
        check(helper, shots >= 6 && shots <= 15, shots + " fireballs");
        check(helper, near(helper, PurpleFireball.class, 32.0).size() == shots, "they should all be in the air");
        check(helper, !dargon.hurt(level.damageSources().inFire(), 10.0f), "fire shouldn't hurt him");
        near(helper, PurpleFireball.class, 32.0).forEach(PurpleFireball::discard);
        dargon.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void elementDragonsDropTheirOwnStaff(GameTestHelper helper) {
        floor(helper);
        for (int element = 0; element < ElementDragon.ELEMENTS; element++) {
            ElementDragon dragon = helper.spawn(ModContent.ELEMENT_DRAGON.get(), new BlockPos(4, 2, 4));
            dragon.setElement(element);
            dragon.hurt(helper.getLevel().damageSources().genericKill(), 100000.0f);
            final net.minecraft.world.item.Item staff = ModContent.elementStaffs().get(element);
            check(helper, near(helper, ItemEntity.class, 12.0).stream().anyMatch(e -> e.getItem().is(staff)), "element " + element + " didn't drop " + staff);
        }
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aStaffFiresItsElement(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        ItemStack staff = new ItemStack(ModContent.WIND_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        staff.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, !near(helper, ElementShot.class, 8.0).isEmpty(), "no shot in the air");
        check(helper, staff.getDamageValue() == 1, "a shot should cost a charge");
        check(helper, staff.getItem().isValidRepairItem(staff, new ItemStack(ModContent.DRAGONS_EYE.get())), "a Dragon's Eye recharges it");
        near(helper, ElementShot.class, 8.0).forEach(ElementShot::discard);
        helper.succeed();
    }

    // ------------------------------------------------------------------ the worm

    @GameTest(template = "empty8")
    public static void wormFoodBuysYourWayPast(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        check(helper, !DoomWorm.ignores(player), "an empty-handed player gets eaten");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.WORM_FOOD.get()));
        check(helper, DoomWorm.ignores(player), "Worm Food should keep it off");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void wormStripsGearIntoItems(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        DoomWorm.strip(player, EquipmentSlot.HEAD);
        check(helper, player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the helmet should be off");
        check(helper, near(helper, ItemEntity.class, 8.0).stream().anyMatch(e -> e.getItem().is(Items.DIAMOND_HELMET)), "and on the ground");
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 40)
    public static void wormCarvesATunnelAndKeepsATrail(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                for (int y = 0; y < 8; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        DoomWorm worm = helper.spawn(ModContent.DOOM_WORM.get(), new BlockPos(4, 4, 4));
        worm.setNoAi(true);
        worm.carve(helper.getLevel());
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 4, 4));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 0, 0));
        worm.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 40)
    public static void wormDiesOnBedrock(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.BEDROCK);
        DoomWorm worm = helper.spawn(ModContent.DOOM_WORM.get(), new BlockPos(4, 3, 4));
        worm.setNoAi(true);
        worm.carve(helper.getLevel());
        check(helper, worm.isDeadOrDying() || worm.isRemoved(), "bedrock should end it");
        helper.succeed();
    }

    // ------------------------------------------------------------------ exploits and losses

    @GameTest(template = "empty8")
    public static void aShrunkPlayerKeepsTheirThingsAndAShrunkCowDoesnt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModContent.SHRUNK, 200));
        List<ItemEntity> drops = new java.util.ArrayList<>();
        drops.add(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), new ItemStack(Items.DIAMOND)));
        var event = new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(player, level.damageSources().generic(), drops, false);
        danger.orespawn.dangerzone.GameEvents.onDrops(event);
        check(helper, event.getDrops().size() == 1, "a shrunk player's inventory should still drop");
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModContent.SHRUNK, 200));
        List<ItemEntity> beef = new java.util.ArrayList<>();
        beef.add(new ItemEntity(level, cow.getX(), cow.getY(), cow.getZ(), new ItemStack(Items.BEEF)));
        var cowEvent = new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(cow, level.damageSources().generic(), beef, false);
        danger.orespawn.dangerzone.GameEvents.onDrops(cowEvent);
        check(helper, cowEvent.getDrops().isEmpty(), "a shrunk cow drops nothing");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void theKingsBlockGivesNothingBack(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        var drops = net.minecraft.world.level.block.Block.getDrops(ModContent.KRAKEN_KING_SPAWNER.get().defaultBlockState(), helper.getLevel(), pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE));
        check(helper, drops.isEmpty(), "it dropped " + drops);
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aBabySitterWithNoKillerWarpsNobody(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        BabySitter sitter = helper.spawn(ModContent.BABY_SITTER.get(), new BlockPos(4, 1, 4));
        sitter.kill();
        check(helper, level.getEntitiesOfClass(DelayedEvent.class, sitter.getBoundingBox().inflate(2.0)).isEmpty(), "nobody killed her, so nobody should be sent anywhere");
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aKingRemembersThePrincessAcrossASave(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = MockPlayers.inLevel(helper);
        KrakenKing king = helper.spawn(ModContent.KRAKEN_KING.get(), new BlockPos(4, 3, 4));
        king.hurt(level.damageSources().playerAttack(player), 1.0f);
        var saved = new net.minecraft.nbt.CompoundTag();
        king.addAdditionalSaveData(saved);
        check(helper, saved.getBoolean("PrincessCame"), "the save should record that she came");
        KrakenKing after = ModContent.KRAKEN_KING.get().create(level);
        after.moveTo(king.getX(), king.getY(), king.getZ());
        after.readAdditionalSaveData(saved);
        level.addFreshEntity(after);
        int before = near(helper, PrincessYoungAdult.class, 32.0).size();
        after.invulnerableTime = 0;
        after.hurt(level.damageSources().playerAttack(player), 1.0f);
        check(helper, near(helper, PrincessYoungAdult.class, 32.0).size() == before, "a reloaded King shouldn't call another");
        near(helper, PrincessYoungAdult.class, 32.0).forEach(net.minecraft.world.entity.Entity::discard);
        king.discard();
        after.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8", timeoutTicks = 40)
    public static void aKingDoesntGetBoredAndLeave(GameTestHelper helper) {
        floor(helper);
        KrakenKing king = helper.spawn(ModContent.KRAKEN_KING.get(), new BlockPos(4, 3, 4));
        var saved = new net.minecraft.nbt.CompoundTag();
        king.addAdditionalSaveData(saved);
        saved.putInt("LongEnough", 0);
        king.readAdditionalSaveData(saved);
        helper.runAfterDelay(3, () -> {
            var later = new net.minecraft.nbt.CompoundTag();
            king.addAdditionalSaveData(later);
            check(helper, later.getInt("LongEnough") > 3000, "his patience should be full, was " + later.getInt("LongEnough"));
            king.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty8")
    public static void aGuardRemembersWhatItGuards(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ElementDragon dragon = helper.spawn(ModContent.ELEMENT_DRAGON.get(), new BlockPos(4, 3, 4));
        BlockPos home = helper.absolutePos(new BlockPos(1, 1, 1));
        dragon.guardHome(home);
        var saved = new net.minecraft.nbt.CompoundTag();
        dragon.addAdditionalSaveData(saved);
        ElementDragon after = ModContent.ELEMENT_DRAGON.get().create(level);
        after.readAdditionalSaveData(saved);
        var again = new net.minecraft.nbt.CompoundTag();
        after.addAdditionalSaveData(again);
        check(helper, again.getLong("Home") == home.asLong(), "the home should survive a save");
        dragon.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aDiamondDoesntTurnDargonIntoATeenPrince(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = MockPlayers.inLevel(helper);
        Dargon dargon = helper.spawn(ModContent.DARGON.get(), new BlockPos(4, 1, 4));
        dargon.tame(player);
        player.moveTo(dargon.position());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        check(helper, dargon.mobInteract(player, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS, "a diamond should do nothing to him");
        check(helper, !dargon.isRemoved() && player.getMainHandItem().getCount() == 1, "he should still be there and the diamond unspent");
        dargon.discard();
        helper.succeed();
    }

    @GameTest(template = "empty8")
    public static void aStrangersDiamondBlockDoesntStealATeenPrincess(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = MockPlayers.inLevel(helper);
        ServerPlayer stranger = MockPlayers.inLevel(helper);
        var teen = helper.spawn(ModContent.PRINCESS_TEEN.get(), new BlockPos(4, 1, 4));
        teen.tame(owner);
        stranger.moveTo(teen.position());
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_BLOCK));
        check(helper, teen.mobInteract(stranger, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS, "a stranger's block should do nothing");
        check(helper, teen.isOwnedBy(owner) && stranger.getMainHandItem().getCount() == 1, "she should still be the owner's and the block unspent");
        teen.discard();
        helper.succeed();
    }
}
