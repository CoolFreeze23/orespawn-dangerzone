package danger.orespawn.dangerzone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import danger.orespawn.dangerzone.entity.Trophy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Draws the real creature through its real renderer, shrunk to fit a block and spinning. No model of its own. */
public class TrophyRenderer extends EntityRenderer<Trophy> {
    private static final Logger LOGGER = LoggerFactory.getLogger("OreSpawnDangerZone");
    private static final float SIZE = 0.8f;

    private final Map<String, Entity> subjects = new HashMap<>();
    private final Set<String> broken = new HashSet<>();
    private Level seenIn;

    public TrophyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Trophy trophy, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String id = trophy.subject();
        if (id.isEmpty() || this.broken.contains(id)) {
            return;
        }
        // Every cached creature holds on to its world, so none of them outlives it.
        if (this.seenIn != trophy.level()) {
            this.seenIn = trophy.level();
            this.subjects.clear();
        }
        Entity subject = this.subjects.get(id);
        if (subject == null) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            subject = location == null ? null : BuiltInRegistries.ENTITY_TYPE.get(location).create(trophy.level());
            if (subject == null) {
                this.broken.add(id);
                return;
            }
            this.subjects.put(id, subject);
        }
        subject.tickCount = trophy.tickCount;
        float longest = Math.max(subject.getBbWidth(), subject.getBbHeight());
        float scale = SIZE / Math.max(0.01f, longest);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((trophy.tickCount + partialTick) * 2.0f));
        pose.scale(scale, scale, scale);
        try {
            this.entityRenderDispatcher.getRenderer(subject).render(subject, 0.0f, partialTick, pose, buffers, light);
        } catch (RuntimeException e) {
            // A creature whose renderer needs a live world only loses its trophy's picture, once.
            LOGGER.warn("Can't draw a trophy of {}", id, e);
            this.broken.add(id);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Trophy trophy) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
