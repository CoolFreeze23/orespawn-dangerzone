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
    private static final int FULL_BRIGHT = 0xF000F0;

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
    private boolean bright;
    private float alpha = 1.0F;

    protected DzModel() {
    }

    protected DzModel(java.util.function.Function<net.minecraft.resources.ResourceLocation, net.minecraft.client.renderer.RenderType> renderType) {
        super(renderType);
    }

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
        int tint = this.color;
        if (this.alpha < 1.0F) {
            int a = (int) (((tint >>> 24) & 0xFF) * this.alpha);
            tint = (a << 24) | (tint & 0xFFFFFF);
        }
        part.render(this.poseStack, this.consumer, this.bright ? FULL_BRIGHT : this.light, this.overlay, tint);
    }

    /** DangerZone's "setBrightness": parts drawn while this is on glow. */
    protected final void fullBright(boolean on) {
        this.bright = on;
    }

    /** DangerZone's shader alpha: parts drawn while this is below 1 are see-through. */
    protected final void alpha(float value) {
        this.alpha = value;
    }

    protected final boolean attacking() {
        if (this.entity instanceof danger.orespawn.dangerzone.entity.DzAnimated animated && !(this.entity instanceof Mob m && m.isAggressive())) {
            return animated.dzAttacking();
        }
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
