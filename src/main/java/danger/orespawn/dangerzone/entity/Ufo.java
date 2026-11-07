package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The mothership. It hangs at height 200 over its target and launches Fighters, and Carriers full of Martians. It
 * never attacks itself, and DangerZone puts no limit on what it launches, so here there is one.
 */
public class Ufo extends Monster implements DzAnimated {
    public static final double HOVER = 200.0;
    private static final double SEARCH = 190.0;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final int[] blinker = new int[8];
    private float angle;
    private Vec3 destination;

    public Ufo(EntityType<? extends Ufo> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, SEARCH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoAi() || !(level() instanceof ServerLevel level)) {
            return;
        }
        Player target = level.getNearestPlayer(getX(), getY(), getZ(), SEARCH, p -> !((Player) p).isCreative() && !p.isSpectator());
        if (target != null) {
            setAggressive(true);
            this.destination = new Vec3(target.getX(), hoverHeight(level), target.getZ());
            if (this.random.nextInt(20) == 1) {
                launch(level);
            }
        } else {
            setAggressive(false);
            if (this.destination == null || this.random.nextInt(50) == 1) {
                this.destination = new Vec3(getX() + this.random.nextInt(35) - this.random.nextInt(35),
                        hoverHeight(level) + this.random.nextInt(7) - this.random.nextInt(7), getZ() + this.random.nextInt(35) - this.random.nextInt(35));
            }
        }
        Vec3 to = this.destination.subtract(position());
        double distance = to.length();
        setDeltaMovement(distance < 1.0 ? Vec3.ZERO : to.scale(Math.min(0.45, distance) / distance));
        this.hasImpulse = true;
        this.bossBar.setProgress(getHealth() / getMaxHealth());
    }

    private double hoverHeight(ServerLevel level) {
        return Math.min(HOVER, level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 8.0);
    }

    /** Sends out one craft, up to the configured limit. Returns whether one left. Public for the tests. */
    public boolean launch(ServerLevel level) {
        int craft = Summons.countNear(this, 128.0, e -> e instanceof UfoFighter || e instanceof UfoCarrier);
        if (craft >= DzConfig.UFO_MAX_CRAFT.get()) {
            return false;
        }
        EntityType<? extends Mob> type = this.random.nextInt(3) == 1 ? ModContent.UFO_CARRIER.get() : ModContent.UFO_FIGHTER.get();
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(getX(), getY() - 5.0, getZ(), this.random.nextFloat() * 360.0f, 0.0f);
        return level.addFreshEntity(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float getNewangle() {
        this.angle = (this.angle + 0.1f) % 360.0f;
        return (float) Math.toRadians(this.angle);
    }

    @Override
    public boolean getBlink(int i) {
        this.blinker[i]++;
        if (this.blinker[i] < 60 + i / 2) {
            return false;
        }
        if (this.blinker[i] >= 120 + i) {
            this.blinker[i] = 0;
        }
        return true;
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
