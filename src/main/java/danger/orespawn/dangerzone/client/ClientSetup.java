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
import danger.orespawn.dangerzone.client.model.DzBabyNopeModel;
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
