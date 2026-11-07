package danger.orespawn.dangerzone;

import danger.orespawn.dangerzone.effect.ShrunkEffect;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BloodlustAnt;
import danger.orespawn.dangerzone.entity.NastysaurusPrime;
import danger.orespawn.dangerzone.entity.Needlenose;
import danger.orespawn.dangerzone.entity.NurseBug;
import danger.orespawn.dangerzone.entity.ShrinkBolt;
import danger.orespawn.dangerzone.item.DimensionStaffItem;
import danger.orespawn.dangerzone.item.DragonsEyeSwordItem;
import danger.orespawn.dangerzone.item.ShrinkRayItem;
import danger.orespawn.dangerzone.recipe.DuctTapeRepairRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModContent {
    private static final String MOD = OreSpawnDangerZone.MOD_ID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD);

    // Sizes are DangerZone's.
    public static final DeferredHolder<EntityType<?>, EntityType<NastysaurusPrime>> NASTYSAURUS_PRIME = ENTITIES.register("nastysaurus_prime",
            () -> EntityType.Builder.of(NastysaurusPrime::new, MobCategory.MONSTER).sized(2.55f, 4.95f).clientTrackingRange(12).build("nastysaurus_prime"));
    public static final DeferredHolder<EntityType<?>, EntityType<Needlenose>> NEEDLENOSE = ENTITIES.register("needlenose",
            () -> EntityType.Builder.of(Needlenose::new, MobCategory.MONSTER).sized(0.85f, 1.55f).clientTrackingRange(8).build("needlenose"));
    public static final DeferredHolder<EntityType<?>, EntityType<NurseBug>> NURSE_BUG = ENTITIES.register("nurse_bug",
            () -> EntityType.Builder.of(NurseBug::new, MobCategory.AMBIENT).sized(2.75f, 3.75f).clientTrackingRange(10).build("nurse_bug"));
    public static final DeferredHolder<EntityType<?>, EntityType<AntQueen>> ANT_QUEEN = ENTITIES.register("ant_queen",
            () -> EntityType.Builder.of(AntQueen::new, MobCategory.MONSTER).sized(8.75f, 8.0f).clientTrackingRange(16).build("ant_queen"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodlustAnt>> BLOODLUST_ANT = ENTITIES.register("bloodlust_ant",
            () -> EntityType.Builder.of(BloodlustAnt::new, MobCategory.MONSTER).sized(2.25f, 2.13f).clientTrackingRange(10).build("bloodlust_ant"));
    public static final DeferredHolder<EntityType<?>, EntityType<ShrinkBolt>> SHRINK_BOLT = ENTITIES.register("shrink_bolt",
            () -> EntityType.Builder.<ShrinkBolt>of(ShrinkBolt::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(4).build("shrink_bolt"));

    public static final DeferredHolder<MobEffect, ShrunkEffect> SHRUNK = EFFECTS.register("shrunk", ShrunkEffect::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NEEDLENOSE_AMBIENT = sound("needlenose.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NEEDLENOSE_HURT = sound("needlenose.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NEEDLENOSE_DEATH = sound("needlenose.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NURSE_AMBIENT = sound("nurse_bug.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NURSE_HURT = sound("nurse_bug.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_QUEEN_HURT = sound("ant_queen.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_QUEEN_DEATH = sound("ant_queen.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_QUEEN_ATTACK = sound("ant_queen.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_BLOODLUST_AMBIENT = sound("bloodlust_ant.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_BLOODLUST_ATTACK = sound("bloodlust_ant.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_BLOODLUST_DEATH = sound("bloodlust_ant.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_RAY = sound("shrink_ray.shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_TELEPORT = sound("item.teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_TAPE = sound("duct_tape.use");

    /** DangerZone's 6000 uses and 85 damage, with damage on the port's scale: its Ultimate Sword is 100 there, 36 here. */
    public static final Tier DRAGONS_EYE_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 6000, 15.0f, 26.0f, 50,
            () -> Ingredient.of(ModContent.DRAGONS_EYE.get()));

    public static final DeferredItem<Item> DRAGONS_EYE = ITEMS.registerSimpleItem("dragons_eye", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<DimensionStaffItem> DIMENSION_STAFF = ITEMS.register("dimension_staff",
            () -> new DimensionStaffItem(new Item.Properties().durability(2000).rarity(Rarity.EPIC)));
    public static final DeferredItem<DragonsEyeSwordItem> DRAGONS_EYE_SWORD = ITEMS.register("dragons_eye_sword",
            () -> new DragonsEyeSwordItem(DRAGONS_EYE_TIER, new Item.Properties().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(DRAGONS_EYE_TIER, 3, -2.4f))));
    public static final DeferredItem<ShrinkRayItem> SHRINK_RAY = ITEMS.register("shrink_ray",
            () -> new ShrinkRayItem(new Item.Properties().durability(500).rarity(Rarity.RARE)));

    public static final DeferredItem<DeferredSpawnEggItem> NASTYSAURUS_PRIME_EGG = egg("nastysaurus_prime", NASTYSAURUS_PRIME);
    public static final DeferredItem<DeferredSpawnEggItem> NEEDLENOSE_EGG = egg("needlenose", NEEDLENOSE);
    public static final DeferredItem<DeferredSpawnEggItem> NURSE_BUG_EGG = egg("nurse_bug", NURSE_BUG);
    public static final DeferredItem<DeferredSpawnEggItem> ANT_QUEEN_EGG = egg("ant_queen", ANT_QUEEN);
    public static final DeferredItem<DeferredSpawnEggItem> BLOODLUST_ANT_EGG = egg("bloodlust_ant", BLOODLUST_ANT);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DuctTapeRepairRecipe>> DUCT_TAPE_REPAIR =
            RECIPES.register("duct_tape_repair", () -> new SimpleCraftingRecipeSerializer<>(DuctTapeRepairRecipe::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.orespawn_dangerzone"))
            .icon(() -> DIMENSION_STAFF.get().getDefaultInstance())
            .displayItems((params, out) -> ITEMS.getEntries().forEach(item -> out.accept(item.get())))
            .build());

    public static final TagKey<EntityType<?>> SHRINK_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("shrink_immune"));
    public static final TagKey<EntityType<?>> DROPS_DRAGONS_EYE = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("drops_dragons_eye"));
    public static final TagKey<Item> NOT_TAPEABLE = TagKey.create(Registries.ITEM, OreSpawnDangerZone.id("not_tapeable"));

    private ModContent() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(OreSpawnDangerZone.id(name)));
    }

    private static <T extends Mob> DeferredItem<DeferredSpawnEggItem> egg(String name, Supplier<EntityType<T>> type) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, 0x6A1B1B, 0xE8D8A0, new Item.Properties()));
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        SOUNDS.register(modBus);
        RECIPES.register(modBus);
        TABS.register(modBus);
        modBus.addListener(ModContent::attributes);
        modBus.addListener(ModContent::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(NASTYSAURUS_PRIME.get(), NastysaurusPrime.createPrimeAttributes().build());
        event.put(NEEDLENOSE.get(), Needlenose.createAttributes().build());
        event.put(NURSE_BUG.get(), NurseBug.createAttributes().build());
        event.put(ANT_QUEEN.get(), AntQueen.createAttributes().build());
        event.put(BLOODLUST_ANT.get(), BloodlustAnt.createAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(NASTYSAURUS_PRIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NEEDLENOSE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLOODLUST_ANT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // The Queen stands in the open by day or night in DangerZone: PERMANENT and not tied to the dark.
        event.register(ANT_QUEEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NURSE_BUG.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NurseBug::checkNurseSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
