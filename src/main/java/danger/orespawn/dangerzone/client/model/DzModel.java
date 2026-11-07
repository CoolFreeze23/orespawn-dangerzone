package danger.orespawn.dangerzone.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import danger.orespawn.entity.Nastysaurus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Map;

/**
 * Runs a DangerZone model's render method. DangerZone animates while it draws, and some models draw one part
 * several times per frame in different poses (an ant's single leg becomes six), so posing can't be separated into
 * setupAnim: the arguments are kept and the original code runs from renderToBuffer.
 *
 * <p>DangerZone's arguments are (ticks alive, speed, head pitch, head yaw, roll).
 */
public abstract class DzModel<T extends Entity> extends EntityModel<T> {
    private T entity;
    private float ticks;
    private float speed;
    private float headPitch;
    private float headYaw;

    private PoseStack poseStack;
    private VertexConsumer consumer;
    private int light;
    private int overlay;
    private int color;

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.entity = entity;
        this.ticks = ageInTicks;
        this.speed = limbSwingAmount;
        this.headPitch = headPitch;
        // DangerZone turns head parts by -yaw; Minecraft by +yaw.
        this.headYaw = -netHeadYaw;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        this.poseStack = poseStack;
        this.consumer = consumer;
        this.light = light;
        this.overlay = overlay;
        this.color = color;
        if (this.entity != null) {
            animateAndDraw(this.entity, this.ticks, this.speed, this.headPitch, this.headYaw, 0.0F);
        }
    }

    protected abstract void animateAndDraw(T entity, float f, float f1, float f2, float f3, float f4);

    protected final void draw(ModelPart part) {
        part.render(this.poseStack, this.consumer, this.light, this.overlay, this.color);
    }

    protected final boolean attacking() {
        if (this.entity instanceof Nastysaurus nastysaurus) {
            return nastysaurus.getAttacking() != 0;
        }
        return this.entity instanceof Mob mob && mob.isAggressive();
    }

    /** A part already welded into its parent's mesh: the original still writes its angles, to no effect. */
    protected static ModelPart detached(float x, float y, float z, float xRot, float yRot, float zRot) {
        ModelPart part = new ModelPart(List.of(), Map.of());
        part.setPos(x, y, z);
        part.setRotation(xRot, yRot, zRot);
        return part;
    }
}
