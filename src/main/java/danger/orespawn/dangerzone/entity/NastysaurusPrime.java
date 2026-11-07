package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.Nastysaurus;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * DangerZone's boss of the Nastysaurus line: it fights like the port's Nastysaurus and calls Needlenoses to the
 * midpoint between itself and whatever it bites. DangerZone has it at twice a Nastysaurus's health and 1.7 times its
 * bite, kept here against the port's 200 and 32.
 */
public class NastysaurusPrime extends Nastysaurus {
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public NastysaurusPrime(EntityType<? extends NastysaurusPrime> type, Level level) {
        super(type, level);
        this.xpReward = 699;
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
    }

    public static AttributeSupplier.Builder createPrimeAttributes() {
        return Nastysaurus.createAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 54.0)
                .add(Attributes.FOLLOW_RANGE, 30.0);
    }

    /** The port's Nastysaurus scan knows nothing of a Needlenose, so its own pack is turned away here. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof Needlenose ? null : target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity victim && this.level() instanceof ServerLevel level && this.random.nextInt(3) == 1) {
            callPack(level, victim);
        }
        return hit;
    }

    /** Calls one Needlenose toward the victim, up to the configured pack size. Returns whether one came. */
    public boolean callPack(ServerLevel level, LivingEntity victim) {
        if (Summons.countNear(this, 32.0, e -> e instanceof Needlenose) >= DzConfig.PRIME_MAX_PACK.get()) {
            return false;
        }
        double x = (getX() + victim.getX()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
        double y = (getY() + victim.getY()) / 2.0;
        double z = (getZ() + victim.getZ()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
        return Summons.spawnAt(level, ModContent.NEEDLENOSE.get(), x, y, z, victim);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }
}
