package danger.orespawn.dangerzone;

import danger.orespawn.dangerzone.effect.ShrunkEffect;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BabyNope;
import danger.orespawn.dangerzone.entity.BabySitter;
import danger.orespawn.dangerzone.entity.ChaosDemon;
import danger.orespawn.dangerzone.entity.Dargon;
import danger.orespawn.dangerzone.entity.DelayedEvent;
import danger.orespawn.dangerzone.entity.DoomWorm;
import danger.orespawn.dangerzone.entity.ElementDragon;
import danger.orespawn.dangerzone.entity.ElementShot;
import danger.orespawn.dangerzone.entity.KrakenKing;
import danger.orespawn.dangerzone.entity.PortalAnt;
import danger.orespawn.dangerzone.entity.PurpleFireball;
import danger.orespawn.dangerzone.entity.SquidGuard;
import danger.orespawn.dangerzone.item.ElementStaffItem;
import danger.orespawn.dangerzone.world.DzBlocks;
import danger.orespawn.dangerzone.world.DzFeatures;
import danger.orespawn.dangerzone.world.DzPlaces;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import danger.orespawn.dangerzone.entity.Annoyance;
import danger.orespawn.dangerzone.entity.DzBeast;
import danger.orespawn.dangerzone.entity.Varmint;
import danger.orespawn.dangerzone.item.MorphStaffItem;
import danger.orespawn.dangerzone.morph.Morph;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class ModContent {
    private static final String MOD = OreSpawnDangerZone.MOD_ID;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MOD);
    private static final Map<DzBeast.Spec, DeferredHolder<EntityType<?>, EntityType<DzBeast>>> BEASTS = new LinkedHashMap<>();

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

    public static final DeferredHolder<EntityType<?>, EntityType<ChaosDemon>> CHAOS_DEMON = ENTITIES.register("chaos_demon",
            () -> EntityType.Builder.of(ChaosDemon::new, MobCategory.MONSTER).sized(1.75f, 3.2f).clientTrackingRange(12).build("chaos_demon"));
    public static final DeferredHolder<EntityType<?>, EntityType<BabySitter>> BABY_SITTER = ENTITIES.register("baby_sitter",
            () -> EntityType.Builder.of(BabySitter::new, MobCategory.MONSTER).sized(3.0f, 14.0f).clientTrackingRange(16).build("baby_sitter"));
    public static final DeferredHolder<EntityType<?>, EntityType<DelayedEvent>> DELAYED_EVENT = ENTITIES.register("delayed_event",
            () -> EntityType.Builder.<DelayedEvent>of(DelayedEvent::new, MobCategory.MISC).sized(0.1f, 0.1f).clientTrackingRange(2).build("delayed_event"));
    public static final DeferredHolder<EntityType<?>, EntityType<KrakenKing>> KRAKEN_KING = ENTITIES.register("kraken_king",
            () -> EntityType.Builder.of(KrakenKing::new, MobCategory.MONSTER).sized(15.75f, 4.75f).clientTrackingRange(32).build("kraken_king"));
    public static final DeferredHolder<EntityType<?>, EntityType<SquidGuard>> SQUID_GUARD = ENTITIES.register("squid_guard",
            () -> EntityType.Builder.of(SquidGuard::new, MobCategory.MONSTER).sized(1.75f, 1.55f).clientTrackingRange(10).build("squid_guard"));
    public static final DeferredHolder<EntityType<?>, EntityType<Dargon>> DARGON = ENTITIES.register("dargon",
            () -> EntityType.Builder.of(Dargon::new, MobCategory.MONSTER).sized(22.75f, 20.75f).fireImmune().clientTrackingRange(32).build("dargon"));
    public static final DeferredHolder<EntityType<?>, EntityType<ElementDragon>> ELEMENT_DRAGON = ENTITIES.register("element_dragon",
            () -> EntityType.Builder.of(ElementDragon::new, MobCategory.MONSTER).sized(4.25f, 3.25f).clientTrackingRange(16).build("element_dragon"));
    public static final DeferredHolder<EntityType<?>, EntityType<ElementShot>> ELEMENT_SHOT = ENTITIES.register("element_shot",
            () -> EntityType.Builder.<ElementShot>of(ElementShot::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(4).build("element_shot"));
    public static final DeferredHolder<EntityType<?>, EntityType<PurpleFireball>> PURPLE_FIREBALL = ENTITIES.register("purple_fireball",
            () -> EntityType.Builder.<PurpleFireball>of(PurpleFireball::new, MobCategory.MISC).sized(1.0f, 1.0f).fireImmune().clientTrackingRange(16).updateInterval(2).build("purple_fireball"));
    public static final DeferredHolder<EntityType<?>, EntityType<DoomWorm>> DOOM_WORM = ENTITIES.register("doom_worm",
            () -> EntityType.Builder.of(DoomWorm::new, MobCategory.MONSTER).sized(4.5f, 4.5f).clientTrackingRange(24).build("doom_worm"));
    public static final DeferredHolder<EntityType<?>, EntityType<PortalAnt>> DOOM_ANT = ENTITIES.register("doom_ant",
            () -> EntityType.Builder.<PortalAnt>of((type, level) -> new PortalAnt(type, level, DzPlaces.DOOM), MobCategory.CREATURE).sized(0.5f, 0.3f).clientTrackingRange(8).build("doom_ant"));
    public static final DeferredHolder<EntityType<?>, EntityType<PortalAnt>> DRANT = ENTITIES.register("drant",
            () -> EntityType.Builder.<PortalAnt>of((type, level) -> new PortalAnt(type, level, DzPlaces.DRAGONS_LAIR), MobCategory.CREATURE).sized(0.5f, 0.3f).clientTrackingRange(8).build("drant"));

    public static final DeferredHolder<EntityType<?>, EntityType<Annoyance>> ANNOYANCE = ENTITIES.register("annoyance",
            () -> EntityType.Builder.of(Annoyance::new, MobCategory.MONSTER).sized(0.95f, 0.85f).fireImmune().clientTrackingRange(10).build("annoyance"));
    public static final DeferredHolder<EntityType<?>, EntityType<Varmint>> VARMINT = ENTITIES.register("varmint",
            () -> EntityType.Builder.<Varmint>of(Varmint::varmint, MobCategory.MONSTER).sized(0.95f, 0.85f).fireImmune().clientTrackingRange(10).build("varmint"));
    public static final DeferredHolder<EntityType<?>, EntityType<Varmint>> VERMIN = ENTITIES.register("vermin",
            () -> EntityType.Builder.<Varmint>of(Varmint::vermin, MobCategory.MONSTER).sized(0.65f, 0.55f).fireImmune().clientTrackingRange(10).build("vermin"));
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> FLATTOP = beast(DzBeast.FLATTOP);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> HYENA = beast(DzBeast.HYENA);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> LAVA_LIZARD = beast(DzBeast.LAVA_LIZARD);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> LAZER_LIZARD = beast(DzBeast.LAZER_LIZARD);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> LIGHTNING_LIZARD = beast(DzBeast.LIGHTNING_LIZARD);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> RAGING_BULL = beast(DzBeast.RAGING_BULL);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> RATTLESNAKE = beast(DzBeast.RATTLESNAKE);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> ROLLY_OUCHY = beast(DzBeast.ROLLY_OUCHY);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> SKINNY_THING = beast(DzBeast.SKINNY_THING);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> V = beast(DzBeast.V);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> WARTHOG = beast(DzBeast.WARTHOG);
    public static final DeferredHolder<EntityType<?>, EntityType<DzBeast>> WOOLLY_MAMMOTH = beast(DzBeast.WOOLLY_MAMMOTH);

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
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_SITTER_AMBIENT = sound("baby_sitter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_SITTER_HURT = sound("baby_sitter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_SITTER_DEATH = sound("baby_sitter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ELEMENT_AMBIENT = sound("element_dragon.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ELEMENT_HURT = sound("element_dragon.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ELEMENT_DEATH = sound("element_dragon.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_WORM_AMBIENT = sound("doom_worm.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ANNOYANCE_AMBIENT = sound("annoyance.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ANNOYANCE_HURT = sound("annoyance.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_ANNOYANCE_DEATH = sound("annoyance.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_VARMINT_AMBIENT = sound("varmint.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_VARMINT_HURT = sound("varmint.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_VARMINT_DEATH = sound("varmint.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_MORPH = sound("item.morph");
    public static final DeferredHolder<SoundEvent, SoundEvent> SND_UNMORPH = sound("item.unmorph");
    static {
        java.util.Set<String> voices = new java.util.HashSet<>();
        for (DzBeast.Spec spec : DzBeast.ALL) {
            if (voices.add(spec.voice())) {
                sound(spec.voice() + ".hurt");
                sound(spec.voice() + ".death");
                if (spec.ambient()) {
                    sound(spec.voice() + ".ambient");
                }
            }
        }
    }

    public static final DeferredBlock<Block> DARK_STONE = BLOCKS.registerSimpleBlock("dark_stone", stone(1.5f, 6.0f));
    public static final DeferredBlock<Block> DARK_DIRT = BLOCKS.registerSimpleBlock("dark_dirt", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5f).sound(SoundType.GRAVEL));
    public static final DeferredBlock<RotatedPillarBlock> DARK_LOG = BLOCKS.registerBlock("dark_log", RotatedPillarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f).sound(SoundType.WOOD));
    public static final DeferredBlock<LeavesBlock> DARK_LEAVES = BLOCKS.registerBlock("dark_leaves", LeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES));
    public static final DeferredBlock<Block> FOSSIL_BONE = BLOCKS.registerSimpleBlock("fossil_bone", stone(1.5f, 6.0f));
    public static final DeferredBlock<Block> DRAGONS_EYE_STONE = BLOCKS.registerSimpleBlock("dragons_eye_stone", stone(3.0f, 9.0f));
    public static final DeferredBlock<DzBlocks.ExtremeTorch> EXTREME_TORCH = BLOCKS.registerBlock("extreme_torch", DzBlocks.ExtremeTorch::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).lightLevel(state -> 15));
    public static final DeferredBlock<DzBlocks.KingSpawner> KRAKEN_KING_SPAWNER = BLOCKS.registerBlock("kraken_king_spawner", DzBlocks.KingSpawner::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).randomTicks());
    public static final DeferredBlock<Block> DARGON_STONE = BLOCKS.registerSimpleBlock("dargon_stone", stone(4.0f, 1200.0f));
    public static final DeferredBlock<Block> DARGON_STONE_DARK = BLOCKS.registerSimpleBlock("dargon_stone_dark", stone(4.0f, 1200.0f));
    public static final DeferredBlock<RotatedPillarBlock> DOOM_LOG = BLOCKS.registerBlock("doom_log", RotatedPillarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f).sound(SoundType.WOOD));
    public static final DeferredBlock<LeavesBlock> DOOM_LEAVES = BLOCKS.registerBlock("doom_leaves", LeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES));
    public static final DeferredBlock<Block> DOOM_SAND = BLOCKS.registerSimpleBlock("doom_sand", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FOSSIL_CROSS = FEATURES.register("fossil_cross", DzFeatures.FossilCross::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ELEMENT_PILLAR = FEATURES.register("element_pillar", DzFeatures.ElementPillar::new);

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

    public static final DeferredItem<BlockItem> DARK_STONE_ITEM = ITEMS.registerSimpleBlockItem(DARK_STONE);
    public static final DeferredItem<BlockItem> DARK_DIRT_ITEM = ITEMS.registerSimpleBlockItem(DARK_DIRT);
    public static final DeferredItem<BlockItem> DARK_LOG_ITEM = ITEMS.registerSimpleBlockItem(DARK_LOG);
    public static final DeferredItem<BlockItem> DARK_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(DARK_LEAVES);
    public static final DeferredItem<BlockItem> FOSSIL_BONE_ITEM = ITEMS.registerSimpleBlockItem(FOSSIL_BONE);
    public static final DeferredItem<BlockItem> DRAGONS_EYE_STONE_ITEM = ITEMS.registerSimpleBlockItem(DRAGONS_EYE_STONE);
    public static final DeferredItem<BlockItem> EXTREME_TORCH_ITEM = ITEMS.registerSimpleBlockItem(EXTREME_TORCH);
    public static final DeferredItem<BlockItem> KRAKEN_KING_SPAWNER_ITEM = ITEMS.registerSimpleBlockItem(KRAKEN_KING_SPAWNER);
    public static final DeferredItem<BlockItem> DARGON_STONE_ITEM = ITEMS.registerSimpleBlockItem(DARGON_STONE);
    public static final DeferredItem<BlockItem> DARGON_STONE_DARK_ITEM = ITEMS.registerSimpleBlockItem(DARGON_STONE_DARK);
    public static final DeferredItem<BlockItem> DOOM_LOG_ITEM = ITEMS.registerSimpleBlockItem(DOOM_LOG);
    public static final DeferredItem<BlockItem> DOOM_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(DOOM_LEAVES);
    public static final DeferredItem<BlockItem> DOOM_SAND_ITEM = ITEMS.registerSimpleBlockItem(DOOM_SAND);

    public static final DeferredItem<Item> WORM_FOOD = ITEMS.registerItem("worm_food", Item::new,
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build()));
    public static final DeferredItem<ElementStaffItem> EARTH_STAFF = staff("earth_staff", ElementShot.EARTH);
    public static final DeferredItem<ElementStaffItem> WIND_STAFF = staff("wind_staff", ElementShot.WIND);
    public static final DeferredItem<ElementStaffItem> FIRE_STAFF = staff("fire_staff", ElementShot.FIRE);
    public static final DeferredItem<ElementStaffItem> WATER_STAFF = staff("water_staff", ElementShot.WATER);
    public static final DeferredItem<ElementStaffItem> LIGHTNING_STAFF = staff("lightning_staff", ElementShot.LIGHTNING);
    public static final DeferredItem<ElementStaffItem> PLANT_STAFF = staff("plant_staff", ElementShot.PLANT);

    /** DangerZone's 65 protection and 80000 uses, with protection on the port's scale, just above its Ultimate Helmet. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROYAL_MATERIAL = ARMOR_MATERIALS.register("royal", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 7, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 7, ArmorItem.Type.BODY, 7), 20,
            net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(ModContent.DRAGONS_EYE.get()),
            List.of(new ArmorMaterial.Layer(OreSpawnDangerZone.id("royal_tiara"))), 4.0f, 0.0f));
    public static final DeferredItem<ArmorItem> ROYAL_TIARA = ITEMS.register("royal_tiara",
            () -> new ArmorItem(ROYAL_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(80000).rarity(Rarity.EPIC)));
    /** DangerZone's 25 damage on the port's scale, and its hundred uses. */
    public static final DeferredItem<MorphStaffItem> MORPH_STAFF = ITEMS.register("morph_staff",
            () -> new MorphStaffItem(new Item.Properties().durability(100).rarity(Rarity.EPIC).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build())));

    public static final DeferredItem<DeferredSpawnEggItem> ANNOYANCE_EGG = egg("annoyance", ANNOYANCE);
    public static final DeferredItem<DeferredSpawnEggItem> VARMINT_EGG = egg("varmint", VARMINT);
    public static final DeferredItem<DeferredSpawnEggItem> VERMIN_EGG = egg("vermin", VERMIN);
    static {
        BEASTS.forEach((spec, holder) -> egg(spec.name(), holder));
    }

    public static final DeferredItem<DeferredSpawnEggItem> CHAOS_DEMON_EGG = egg("chaos_demon", CHAOS_DEMON);
    public static final DeferredItem<DeferredSpawnEggItem> BABY_SITTER_EGG = egg("baby_sitter", BABY_SITTER);
    public static final DeferredItem<DeferredSpawnEggItem> KRAKEN_KING_EGG = egg("kraken_king", KRAKEN_KING);
    public static final DeferredItem<DeferredSpawnEggItem> SQUID_GUARD_EGG = egg("squid_guard", SQUID_GUARD);
    public static final DeferredItem<DeferredSpawnEggItem> DARGON_EGG = egg("dargon", DARGON);
    public static final DeferredItem<DeferredSpawnEggItem> ELEMENT_DRAGON_EGG = egg("element_dragon", ELEMENT_DRAGON);
    public static final DeferredItem<DeferredSpawnEggItem> DOOM_WORM_EGG = egg("doom_worm", DOOM_WORM);
    public static final DeferredItem<DeferredSpawnEggItem> DOOM_ANT_EGG = egg("doom_ant", DOOM_ANT);
    public static final DeferredItem<DeferredSpawnEggItem> DRANT_EGG = egg("drant", DRANT);

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

    private static DeferredHolder<EntityType<?>, EntityType<DzBeast>> beast(DzBeast.Spec spec) {
        DeferredHolder<EntityType<?>, EntityType<DzBeast>> holder = ENTITIES.register(spec.name(), () -> {
            EntityType.Builder<DzBeast> builder = EntityType.Builder.<DzBeast>of((type, level) -> new DzBeast(type, level, spec), MobCategory.MONSTER)
                    .sized(spec.width(), spec.height()).clientTrackingRange(spec.width() >= 4.0f ? 16 : 10);
            if (spec.fireproof()) {
                builder.fireImmune();
            }
            return builder.build(spec.name());
        });
        BEASTS.put(spec, holder);
        return holder;
    }

    private static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(hardness, resistance).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    private static DeferredItem<ElementStaffItem> staff(String name, int element) {
        return ITEMS.register(name, () -> new ElementStaffItem(element, new Item.Properties().durability(100).rarity(Rarity.RARE)));
    }

    /** In element order: what each Element Dragon drops. */
    public static java.util.List<? extends Item> elementStaffs() {
        return java.util.List.of(EARTH_STAFF.get(), WIND_STAFF.get(), FIRE_STAFF.get(), WATER_STAFF.get(), LIGHTNING_STAFF.get(), PLANT_STAFF.get());
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(OreSpawnDangerZone.id(name)));
    }

    private static <T extends Mob> DeferredItem<DeferredSpawnEggItem> egg(String name, Supplier<EntityType<T>> type) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, 0x6A1B1B, 0xE8D8A0, new Item.Properties()));
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        FEATURES.register(modBus);
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        SOUNDS.register(modBus);
        RECIPES.register(modBus);
        TABS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        Morph.register(modBus);
        modBus.addListener(ModContent::attributes);
        modBus.addListener(ModContent::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(NASTYSAURUS_PRIME.get(), NastysaurusPrime.createPrimeAttributes().build());
        event.put(NEEDLENOSE.get(), Needlenose.createAttributes().build());
        event.put(NURSE_BUG.get(), NurseBug.createAttributes().build());
        event.put(ANT_QUEEN.get(), AntQueen.createAttributes().build());
        event.put(BLOODLUST_ANT.get(), BloodlustAnt.createAttributes().build());
        event.put(ANNOYANCE.get(), Annoyance.createAttributes().build());
        event.put(VARMINT.get(), Varmint.varmintAttributes().build());
        event.put(VERMIN.get(), Varmint.verminAttributes().build());
        BEASTS.forEach((spec, holder) -> event.put(holder.get(), DzBeast.attributes(spec).build()));
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
        event.put(CHAOS_DEMON.get(), ChaosDemon.createAttributes().build());
        event.put(BABY_SITTER.get(), BabySitter.createAttributes().build());
        event.put(KRAKEN_KING.get(), KrakenKing.createKingAttributes().build());
        event.put(SQUID_GUARD.get(), SquidGuard.createAttributes().build());
        event.put(DARGON.get(), Dargon.createDargonAttributes().build());
        event.put(ELEMENT_DRAGON.get(), ElementDragon.createAttributes().build());
        event.put(DOOM_WORM.get(), DoomWorm.createAttributes().build());
        event.put(DOOM_ANT.get(), PortalAnt.createAttributes().build());
        event.put(DRANT.get(), PortalAnt.createAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        BEASTS.forEach((spec, holder) -> event.register(holder.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spec.byDay() ? Mob::checkMobSpawnRules : Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        event.register(NASTYSAURUS_PRIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(NEEDLENOSE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLOODLUST_ANT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // The Queen stands in the open by day or night in DangerZone: PERMANENT and not tied to the dark.
        event.register(ANT_QUEEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHAOS_DEMON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // It flies, and its realm is mostly sea: any open air will do, over water as well as land.
        event.register(SQUID_GUARD.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos).isAir(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DARGON.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DOOM_WORM.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DOOM_ANT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DRANT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
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
