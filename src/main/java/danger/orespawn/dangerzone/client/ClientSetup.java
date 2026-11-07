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
import danger.orespawn.dangerzone.entity.AntQueen;
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
    }

    private static final class DzRenderer<T extends Mob> extends MobRenderer<T, DzModel<T>> {
        private final ResourceLocation texture;
        private final float scale;

        DzRenderer(EntityRendererProvider.Context context, DzModel<T> model, String texture, float shadow, float scale) {
            super(context, model, shadow);
            this.texture = OreSpawnDangerZone.id("textures/entity/" + texture + ".png");
            this.scale = scale;
        }

        @Override
        protected void scale(T entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(this.scale, this.scale, this.scale);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return this.texture;
        }
    }
}
