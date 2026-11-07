package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** A troop ship: it sinks in beside its target and, at the ground, breaks open and lets out ten Martians. */
public class UfoCarrier extends Monster {
    private static final int TROOPS = 10;

    public UfoCarrier(EntityType<? extends UfoCarrier> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 75;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 75.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 190.0);
    }

    /**
     * Launched from 200 blocks up, well past the distance at which a creature far from every player is cleared away: it
     * would be gone the moment it was made, and the mothership would launch another, and another. A minute to come down.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.tickCount > 1200;
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoAi() || !(level() instanceof ServerLevel level)) {
            return;
        }
        Player target = level.getNearestPlayer(getX(), getY(), getZ(), 190.0, p -> !((Player) p).isCreative() && !p.isSpectator());
        double dx = 0.0;
        double dz = 0.0;
        if (target != null) {
            Vec3 flat = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (flat.length() > 10.0) {
                Vec3 step = flat.normalize().scale(0.3);
                dx = step.x;
                dz = step.z;
            }
        }
        setDeltaMovement(dx, -0.3, dz);
        this.hasImpulse = true;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
        if (getY() <= ground + 1.5 || this.horizontalCollision || this.verticalCollision) {
            land(level);
        }
    }

    /** Breaks open. Returns how many Martians got out. Public for the tests. */
    public int land(ServerLevel level) {
        int present = Summons.countNear(this, 64.0, e -> e instanceof Martian);
        int room = Math.min(TROOPS, DzConfig.UFO_MAX_MARTIANS.get() - present);
        int out = 0;
        for (int i = 0; i < room; i++) {
            double x = getX() + this.random.nextInt(6) - this.random.nextInt(6);
            double z = getZ() + this.random.nextInt(6) - this.random.nextInt(6);
            if (Summons.spawnAt(level, ModContent.MARTIAN.get(), x, getY() + 2.0, z, null)) {
                out++;
            }
        }
        discard();
        return out;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
