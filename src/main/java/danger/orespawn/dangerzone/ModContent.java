package danger.orespawn.dangerzone;

import danger.orespawn.dangerzone.effect.ShrunkEffect;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BabyNope;
import danger.orespawn.dangerzone.entity.Martian;
import danger.orespawn.dangerzone.entity.Nope;
import danger.orespawn.dangerzone.entity.PrincessTeen;
import danger.orespawn.dangerzone.entity.PrincessYoungAdult;
import danger.orespawn.dangerzone.entity.TowerFlag;
import danger.orespawn.dangerzone.entity.Trophy;
import danger.orespawn.dangerzone.entity.Ufo;
import danger.orespawn.dangerzone.entity.UfoCarrier;
import danger.orespawn.dangerzone.entity.UfoFighter;
import danger.orespawn.dangerzone.entity.Volcano;
import danger.orespawn.dangerzone.item.TowerFlagItem;
import danger.orespawn.dangerzone.item.TrophyItem;
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

    public static final DeferredHolder<EntityType<?>, EntityType<TowerFlag>> TOWER_FLAG = ENTITIES.register("tower_flag",
            () -> EntityType.Builder.of(TowerFlag::new, MobCategory.MISC).sized(1.0f, 10.0f).clientTrackingRange(16).build("tower_flag"));
    public static final DeferredHolder<EntityType<?>, EntityType<Trophy>> TROPHY_ENTITY = ENTITIES.register("trophy",
            () -> EntityType.Builder.<Trophy>of(Trophy::new, MobCategory.MISC).sized(0.8f, 0.8f).clientTrackingRange(8).build("trophy"));
    public static final DeferredHolder<EntityType<?>, EntityType<Nope>> NOPE = ENTITIES.register("nope",
            () -> EntityType.Builder.of(Nope::new, MobCategory.MONSTER).sized(8.75f, 7.0f).clientTrackingRange(16).build("nope"));
    public static final DeferredHolder<EntityType<?>, EntityType<BabyNope>> BABY_NOPE = ENTITIES.register("baby_nope",
            () -> EntityType.Builder.of(BabyNope::new, MobCategory.MONSTER).sized(1.25f, 0.95f).clientTrackingRange(8).build("baby_nope"));
    public static final DeferredHolder<EntityType<?>, EntityType<PrincessTeen>> PRINCESS_TEEN = ENTITIES.register("princess_teen",
            () -> EntityType.Builder.of(PrincessTeen::new, MobCategory.CREATURE).sized(3.25f, 3.75f).clientTrackingRange(12).build("princess_teen"));
    public static final DeferredHolder<EntityType<?>, EntityType<PrincessYoungAdult>> PRINCESS_YOUNG_ADULT = ENTITIES.register("princess_young_adult",
            () -> EntityType.Builder.of(PrincessYoungAdult::new, MobCategory.CREATURE).sized(10.75f, 11.75f).clientTrackingRange(16).build("princess_young_adult"));
    public static final DeferredHolder<EntityType<?>, EntityType<Volcano>> VOLCANO = ENTITIES.register("volcano",
            () -> EntityType.Builder.of(Volcano::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8).build("volcano"));
    public static final DeferredHolder<EntityType<?>, EntityType<Ufo>> UFO = ENTITIES.register("ufo",
            () -> EntityType.Builder.of(Ufo::new, MobCategory.MONSTER).sized(16.0f, 4.0f).clientTrackingRange(32).build("ufo"));
    public static final DeferredHolder<EntityType<?>, EntityType<UfoCarrier>> UFO_CARRIER = ENTITIES.register("ufo_carrier",
            () -> EntityType.Builder.of(UfoCarrier::new, MobCategory.MONSTER).sized(6.0f, 2.5f).clientTrackingRange(24).build("ufo_carrier"));
    public static final DeferredHolder<EntityType<?>, EntityType<UfoFighter>> UFO_FIGHTER = ENTITIES.register("ufo_fighter",
            () -> EntityType.Builder.of(UfoFighter::new, MobCategory.MONSTER).sized(2.5f, 2.1f).clientTrackingRange(24).build("ufo_fighter"));
    public static final DeferredHolder<EntityType<?>, EntityType<Martian>> MARTIAN = ENTITIES.register("martian",
            () -> EntityType.Builder.of(Martian::new, MobCategory.MONSTER).sized(0.65f, 1.55f).clientTrackingRange(10).build("martian"));

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
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_NOPE_AMBIENT = sound("nope.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_MARTIAN_HURT = sound("martian.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_MARTIAN_DEATH = sound("martian.death");

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

    public static final DeferredItem<TowerFlagItem> TOWER_FLAG_ITEM = ITEMS.register("tower_flag",
            () -> new TowerFlagItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<TrophyItem> TROPHY = ITEMS.register("trophy",
            () -> new TrophyItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<DeferredSpawnEggItem> NOPE_EGG = egg("nope", NOPE);
    public static final DeferredItem<DeferredSpawnEggItem> BABY_NOPE_EGG = egg("baby_nope", BABY_NOPE);
    public static final DeferredItem<DeferredSpawnEggItem> PRINCESS_TEEN_EGG = egg("princess_teen", PRINCESS_TEEN);
    public static final DeferredItem<DeferredSpawnEggItem> PRINCESS_YOUNG_ADULT_EGG = egg("princess_young_adult", PRINCESS_YOUNG_ADULT);
    public static final DeferredItem<DeferredSpawnEggItem> VOLCANO_EGG = egg("volcano", VOLCANO);
    public static final DeferredItem<DeferredSpawnEggItem> UFO_EGG = egg("ufo", UFO);
    public static final DeferredItem<DeferredSpawnEggItem> UFO_CARRIER_EGG = egg("ufo_carrier", UFO_CARRIER);
    public static final DeferredItem<DeferredSpawnEggItem> UFO_FIGHTER_EGG = egg("ufo_fighter", UFO_FIGHTER);
    public static final DeferredItem<DeferredSpawnEggItem> MARTIAN_EGG = egg("martian", MARTIAN);

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
            .displayItems((params, out) -> ITEMS.getEntries().forEach(item -> {
                // a blank trophy has no creature to be: they only come as drops
                if (item.get() != TROPHY.get()) {
                    out.accept(item.get());
                }
            }))
            .build());

    public static final TagKey<EntityType<?>> SHRINK_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("shrink_immune"));
    public static final TagKey<EntityType<?>> DROPS_DRAGONS_EYE = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("drops_dragons_eye"));
    public static final TagKey<EntityType<?>> TOWER_DEFENSE = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("tower_defense"));
    public static final TagKey<EntityType<?>> NO_TROPHY = TagKey.create(Registries.ENTITY_TYPE, OreSpawnDangerZone.id("no_trophy"));
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
        event.put(TOWER_FLAG.get(), TowerFlag.createAttributes().build());
        event.put(NOPE.get(), Nope.createAttributes().build());
        event.put(BABY_NOPE.get(), BabyNope.createAttributes().build());
        event.put(PRINCESS_TEEN.get(), PrincessTeen.createPrincessAttributes().build());
        event.put(PRINCESS_YOUNG_ADULT.get(), PrincessYoungAdult.createPrincessAttributes().build());
        event.put(VOLCANO.get(), Volcano.createAttributes().build());
        event.put(UFO.get(), Ufo.createAttributes().build());
        event.put(UFO_CARRIER.get(), UfoCarrier.createAttributes().build());
        event.put(UFO_FIGHTER.get(), UfoFighter.createAttributes().build());
        event.put(MARTIAN.get(), Martian.createAttributes().build());
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
        event.register(NOPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BABY_NOPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MARTIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(UFO.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOLCANO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Volcano::checkVolcanoSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NURSE_BUG.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NurseBug::checkNurseSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
