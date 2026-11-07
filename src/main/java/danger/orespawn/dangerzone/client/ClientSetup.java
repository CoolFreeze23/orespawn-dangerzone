package danger.orespawn.dangerzone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.client.model.DzAntQueenModel;
import danger.orespawn.dangerzone.client.model.DzBloodlustAntModel;
import danger.orespawn.dangerzone.client.model.DzModel;
import danger.orespawn.dangerzone.client.model.DzNastysaurusPrimeModel;
import danger.orespawn.dangerzone.client.model.DzNeedlenoseModel;
import danger.orespawn.dangerzone.client.model.DzNurseBugModel;
import danger.orespawn.dangerzone.client.model.DzAntModel;
import danger.orespawn.dangerzone.client.model.DzBabyNopeModel;
import danger.orespawn.dangerzone.client.model.DzBabySitterModel;
import danger.orespawn.dangerzone.client.model.DzChaosDemonModel;
import danger.orespawn.dangerzone.client.model.DzDargonModel;
import danger.orespawn.dangerzone.client.model.DzDoomWormModel;
import danger.orespawn.dangerzone.client.model.DzDrantModel;
import danger.orespawn.dangerzone.client.model.DzElementDragonModel;
import danger.orespawn.dangerzone.client.model.DzKrakenKingModel;
import danger.orespawn.dangerzone.client.model.DzSquidGuardModel;
import danger.orespawn.dangerzone.entity.BabySitter;
import danger.orespawn.dangerzone.entity.ChaosDemon;
import danger.orespawn.dangerzone.entity.Dargon;
import danger.orespawn.dangerzone.entity.DoomWorm;
import danger.orespawn.dangerzone.entity.ElementDragon;
import danger.orespawn.dangerzone.entity.KrakenKing;
import danger.orespawn.dangerzone.entity.PortalAnt;
import danger.orespawn.dangerzone.entity.SquidGuard;
import danger.orespawn.dangerzone.client.model.DzMartianModel;
import danger.orespawn.dangerzone.client.model.DzNopeModel;
import danger.orespawn.dangerzone.client.model.DzPrincessTeenModel;
import danger.orespawn.dangerzone.client.model.DzPrincessYoungAdultModel;
import danger.orespawn.dangerzone.client.model.DzTowerFlagModel;
import danger.orespawn.dangerzone.client.model.DzUfoCarrierModel;
import danger.orespawn.dangerzone.client.model.DzUfoFighterModel;
import danger.orespawn.dangerzone.client.model.DzUfoModel;
import danger.orespawn.dangerzone.entity.AntQueen;
import danger.orespawn.dangerzone.entity.BabyNope;
import danger.orespawn.dangerzone.entity.Martian;
import danger.orespawn.dangerzone.entity.Nope;
import danger.orespawn.dangerzone.entity.PrincessTeen;
import danger.orespawn.dangerzone.entity.PrincessYoungAdult;
import danger.orespawn.dangerzone.entity.TowerFlag;
import danger.orespawn.dangerzone.entity.Ufo;
import danger.orespawn.dangerzone.entity.UfoCarrier;
import danger.orespawn.dangerzone.entity.UfoFighter;
import danger.orespawn.dangerzone.entity.BloodlustAnt;
import danger.orespawn.dangerzone.entity.NastysaurusPrime;
import danger.orespawn.dangerzone.entity.Needlenose;
import danger.orespawn.dangerzone.entity.NurseBug;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private static final ModelLayerLocation PRIME = layer("nastysaurus_prime");
    private static final ModelLayerLocation NEEDLENOSE = layer("needlenose");
    private static final ModelLayerLocation NURSE = layer("nurse_bug");
    private static final ModelLayerLocation QUEEN = layer("ant_queen");
    private static final ModelLayerLocation BLOODLUST = layer("bloodlust_ant");
    private static final ModelLayerLocation FLAG = layer("tower_flag");
    private static final ModelLayerLocation NOPE = layer("nope");
    private static final ModelLayerLocation BABY_NOPE = layer("baby_nope");
    private static final ModelLayerLocation PRINCESS_TEEN = layer("princess_teen");
    private static final ModelLayerLocation PRINCESS_YOUNG_ADULT = layer("princess_young_adult");
    private static final ModelLayerLocation UFO = layer("ufo");
    private static final ModelLayerLocation UFO_CARRIER = layer("ufo_carrier");
    private static final ModelLayerLocation UFO_FIGHTER = layer("ufo_fighter");
    private static final ModelLayerLocation MARTIAN = layer("martian");
    private static final ModelLayerLocation CHAOS_DEMON = layer("chaos_demon");
    private static final ModelLayerLocation BABY_SITTER = layer("baby_sitter");
    private static final ModelLayerLocation KRAKEN_KING = layer("kraken_king");
    private static final ModelLayerLocation SQUID_GUARD = layer("squid_guard");
    private static final ModelLayerLocation DARGON = layer("dargon");
    private static final ModelLayerLocation ELEMENT_DRAGON = layer("element_dragon");
    private static final ModelLayerLocation DOOM_WORM = layer("doom_worm");
    private static final ModelLayerLocation DOOM_ANT = layer("doom_ant");
    private static final ModelLayerLocation DRANT = layer("drant");
    private static final String[] ELEMENT_TEXTURES = {"element_dragon_earth", "element_dragon_wind", "element_dragon_fire",
            "element_dragon_water", "element_dragon_lightning", "element_dragon_plant"};

    private ClientSetup() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(OreSpawnDangerZone.id(name), "main");
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PRIME, DzNastysaurusPrimeModel::createBodyLayer);
        event.registerLayerDefinition(NEEDLENOSE, DzNeedlenoseModel::createBodyLayer);
        event.registerLayerDefinition(NURSE, DzNurseBugModel::createBodyLayer);
        event.registerLayerDefinition(QUEEN, DzAntQueenModel::createBodyLayer);
        event.registerLayerDefinition(BLOODLUST, DzBloodlustAntModel::createBodyLayer);
        event.registerLayerDefinition(FLAG, DzTowerFlagModel::createBodyLayer);
        event.registerLayerDefinition(NOPE, DzNopeModel::createBodyLayer);
        event.registerLayerDefinition(BABY_NOPE, DzBabyNopeModel::createBodyLayer);
        event.registerLayerDefinition(PRINCESS_TEEN, DzPrincessTeenModel::createBodyLayer);
        event.registerLayerDefinition(PRINCESS_YOUNG_ADULT, DzPrincessYoungAdultModel::createBodyLayer);
        event.registerLayerDefinition(UFO, DzUfoModel::createBodyLayer);
        event.registerLayerDefinition(UFO_CARRIER, DzUfoCarrierModel::createBodyLayer);
        event.registerLayerDefinition(UFO_FIGHTER, DzUfoFighterModel::createBodyLayer);
        event.registerLayerDefinition(MARTIAN, DzMartianModel::createBodyLayer);
        event.registerLayerDefinition(CHAOS_DEMON, DzChaosDemonModel::createBodyLayer);
        event.registerLayerDefinition(BABY_SITTER, DzBabySitterModel::createBodyLayer);
        event.registerLayerDefinition(KRAKEN_KING, DzKrakenKingModel::createBodyLayer);
        event.registerLayerDefinition(SQUID_GUARD, DzSquidGuardModel::createBodyLayer);
        event.registerLayerDefinition(DARGON, DzDargonModel::createBodyLayer);
        event.registerLayerDefinition(ELEMENT_DRAGON, DzElementDragonModel::createBodyLayer);
        event.registerLayerDefinition(DOOM_WORM, DzDoomWormModel::createBodyLayer);
        event.registerLayerDefinition(DOOM_ANT, DzAntModel::createBodyLayer);
        event.registerLayerDefinition(DRANT, DzDrantModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.NASTYSAURUS_PRIME.get(), (EntityRendererProvider<NastysaurusPrime>) c ->
                new DzRenderer<>(c, new DzNastysaurusPrimeModel<>(c.bakeLayer(PRIME)), "nastysaurus_prime", 1.5f, DzNastysaurusPrimeModel.SCALE));
        event.registerEntityRenderer(ModContent.NEEDLENOSE.get(), (EntityRendererProvider<Needlenose>) c ->
                new DzRenderer<>(c, new DzNeedlenoseModel<>(c.bakeLayer(NEEDLENOSE)), "needlenose", 0.5f, DzNeedlenoseModel.SCALE));
        event.registerEntityRenderer(ModContent.NURSE_BUG.get(), (EntityRendererProvider<NurseBug>) c ->
                new DzRenderer<>(c, new DzNurseBugModel<>(c.bakeLayer(NURSE)), "nurse_bug", 1.0f, DzNurseBugModel.SCALE));
        event.registerEntityRenderer(ModContent.ANT_QUEEN.get(), (EntityRendererProvider<AntQueen>) c ->
                new DzRenderer<>(c, new DzAntQueenModel<>(c.bakeLayer(QUEEN)), "ant_queen", 4.0f, DzAntQueenModel.SCALE));
        event.registerEntityRenderer(ModContent.BLOODLUST_ANT.get(), (EntityRendererProvider<BloodlustAnt>) c ->
                new DzRenderer<>(c, new DzBloodlustAntModel<>(c.bakeLayer(BLOODLUST)), "bloodlust_ant", 1.0f, DzBloodlustAntModel.SCALE));
        event.registerEntityRenderer(ModContent.SHRINK_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModContent.VOLCANO.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModContent.DELAYED_EVENT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModContent.PURPLE_FIREBALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModContent.ELEMENT_SHOT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModContent.CHAOS_DEMON.get(), (EntityRendererProvider<ChaosDemon>) c ->
                new DzRenderer<>(c, new DzChaosDemonModel<>(c.bakeLayer(CHAOS_DEMON)), "chaos_demon", 1.0f, DzChaosDemonModel.SCALE));
        event.registerEntityRenderer(ModContent.BABY_SITTER.get(), (EntityRendererProvider<BabySitter>) c ->
                new DzRenderer<>(c, new DzBabySitterModel<>(c.bakeLayer(BABY_SITTER)), "baby_sitter", 2.0f, DzBabySitterModel.SCALE));
        event.registerEntityRenderer(ModContent.KRAKEN_KING.get(), (EntityRendererProvider<KrakenKing>) c ->
                new DzRenderer<>(c, new DzKrakenKingModel<>(c.bakeLayer(KRAKEN_KING)), "kraken_king", 0.0f, DzKrakenKingModel.SCALE));
        event.registerEntityRenderer(ModContent.SQUID_GUARD.get(), (EntityRendererProvider<SquidGuard>) c ->
                new DzRenderer<>(c, new DzSquidGuardModel<>(c.bakeLayer(SQUID_GUARD)), "squid_guard", 0.0f, DzSquidGuardModel.SCALE));
        event.registerEntityRenderer(ModContent.DARGON.get(), (EntityRendererProvider<Dargon>) c ->
                new DzRenderer<>(c, new DzDargonModel<>(c.bakeLayer(DARGON)), "dargon", 0.0f, DzDargonModel.SCALE));
        event.registerEntityRenderer(ModContent.ELEMENT_DRAGON.get(), (EntityRendererProvider<ElementDragon>) c ->
                new DzRenderer<>(c, new DzElementDragonModel<>(c.bakeLayer(ELEMENT_DRAGON)), "element_dragon", 0.0f, DzElementDragonModel.SCALE, e -> ELEMENT_TEXTURES[e.element()]));
        event.registerEntityRenderer(ModContent.DOOM_WORM.get(), (EntityRendererProvider<DoomWorm>) c ->
                new DzRenderer<>(c, new DzDoomWormModel<>(c.bakeLayer(DOOM_WORM)), "doom_worm", 0.0f, DzDoomWormModel.SCALE));
        event.registerEntityRenderer(ModContent.DOOM_ANT.get(), (EntityRendererProvider<PortalAnt>) c ->
                new DzRenderer<>(c, new DzAntModel<>(c.bakeLayer(DOOM_ANT)), "doom_ant", 0.2f, DzAntModel.SCALE));
        event.registerEntityRenderer(ModContent.DRANT.get(), (EntityRendererProvider<PortalAnt>) c ->
                new DzRenderer<>(c, new DzDrantModel<>(c.bakeLayer(DRANT)), "drant", 0.2f, DzDrantModel.SCALE));
        event.registerEntityRenderer(ModContent.TROPHY_ENTITY.get(), TrophyRenderer::new);
        event.registerEntityRenderer(ModContent.TOWER_FLAG.get(), (EntityRendererProvider<TowerFlag>) c ->
                new DzRenderer<>(c, new DzTowerFlagModel<>(c.bakeLayer(FLAG)), "tower_flag", 0.5f, DzTowerFlagModel.SCALE));
        event.registerEntityRenderer(ModContent.NOPE.get(), (EntityRendererProvider<Nope>) c ->
                new DzRenderer<>(c, new DzNopeModel<>(c.bakeLayer(NOPE)), "nope", 4.0f, DzNopeModel.SCALE));
        event.registerEntityRenderer(ModContent.BABY_NOPE.get(), (EntityRendererProvider<BabyNope>) c ->
                new DzRenderer<>(c, new DzBabyNopeModel<>(c.bakeLayer(BABY_NOPE)), "baby_nope", 0.5f, DzBabyNopeModel.SCALE));
        event.registerEntityRenderer(ModContent.PRINCESS_TEEN.get(), (EntityRendererProvider<PrincessTeen>) c ->
                new DzRenderer<>(c, new DzPrincessTeenModel<>(c.bakeLayer(PRINCESS_TEEN)), "princess_teen", 2.0f, DzPrincessTeenModel.SCALE, e -> e.getAttacking() != 0 ? "princess_teen_angry" : "princess_teen"));
        event.registerEntityRenderer(ModContent.PRINCESS_YOUNG_ADULT.get(), (EntityRendererProvider<PrincessYoungAdult>) c ->
                new DzRenderer<>(c, new DzPrincessYoungAdultModel<>(c.bakeLayer(PRINCESS_YOUNG_ADULT)), "princess_young_adult", 5.0f, DzPrincessYoungAdultModel.SCALE, e -> e.getAttacking() != 0 ? "princess_young_adult_angry" : "princess_young_adult"));
        event.registerEntityRenderer(ModContent.UFO.get(), (EntityRendererProvider<Ufo>) c ->
                new DzRenderer<>(c, new DzUfoModel<>(c.bakeLayer(UFO)), "ufo", 0.0f, DzUfoModel.SCALE));
        event.registerEntityRenderer(ModContent.UFO_CARRIER.get(), (EntityRendererProvider<UfoCarrier>) c ->
                new DzRenderer<>(c, new DzUfoCarrierModel<>(c.bakeLayer(UFO_CARRIER)), "ufo_carrier", 0.0f, DzUfoCarrierModel.SCALE));
        event.registerEntityRenderer(ModContent.UFO_FIGHTER.get(), (EntityRendererProvider<UfoFighter>) c ->
                new DzRenderer<>(c, new DzUfoFighterModel<>(c.bakeLayer(UFO_FIGHTER)), "ufo_fighter", 0.0f, DzUfoFighterModel.SCALE));
        event.registerEntityRenderer(ModContent.MARTIAN.get(), (EntityRendererProvider<Martian>) c ->
                new DzRenderer<>(c, new DzMartianModel<>(c.bakeLayer(MARTIAN)), "martian", 0.4f, DzMartianModel.SCALE));
    }

    private static final class DzRenderer<T extends Mob> extends MobRenderer<T, DzModel<T>> {
        private final java.util.function.Function<T, String> texture;
        private final float scale;

        DzRenderer(EntityRendererProvider.Context context, DzModel<T> model, String texture, float shadow, float scale) {
            this(context, model, texture, shadow, scale, e -> texture);
        }

        DzRenderer(EntityRendererProvider.Context context, DzModel<T> model, String texture, float shadow, float scale, java.util.function.Function<T, String> textures) {
            super(context, model, shadow);
            this.texture = textures;
            this.scale = scale;
        }

        @Override
        protected void scale(T entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(this.scale, this.scale, this.scale);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return OreSpawnDangerZone.id("textures/entity/" + this.texture.apply(entity) + ".png");
        }
    }
}
