package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * DangerZone's Ant Queen. Each strike can call a Bloodlust Ant to the midpoint of the fight, and a brood ant of any
 * type climbs out beside her. DangerZone's 2200 health and 65 bite, against the 400 and 50 of its Nastysaurus, are
 * kept here in proportion to the port's 200 and 32. At nearly nine blocks wide she doesn't fit any path, so she walks
 * straight at what she wants.
 */
public class AntQueen extends Monster {
    /** The port's ants, DangerZone's nine less the four that live in dimensions the port doesn't have. */
    private static final List<ResourceLocation> BROOD = List.of(
            ResourceLocation.fromNamespaceAndPath("orespawn", "ant"),
            ResourceLocation.fromNamespaceAndPath("orespawn", "red_ant"),
            ResourceLocation.fromNamespaceAndPath("orespawn", "rainbow_ant"),
            ResourceLocation.fromNamespaceAndPath("orespawn", "unstable_ant"),
            ResourceLocation.fromNamespaceAndPath("orespawn", "termite"));

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int strikeCooldown;

    public AntQueen(EntityType<? extends AntQueen> type, Level level) {
        super(type, level);
        this.xpReward = 3200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1100.0)
                .add(Attributes.ATTACK_DAMAGE, 42.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MarchGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, BloodlustAnt.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof BloodlustAnt ? null : target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        setAggressive(target != null);
        if (this.strikeCooldown > 0) {
            this.strikeCooldown--;
        }
        if (target != null && target.isAlive() && this.strikeCooldown == 0 && this.level() instanceof ServerLevel level) {
            double reach = 8.0 + getBbWidth() / 2.0 + target.getBbWidth() / 2.0;
            if (distanceToSqr(target) <= reach * reach) {
                this.strikeCooldown = 25;
                playSound(ModContent.SND_QUEEN_ATTACK.get(), 2.0f, 1.0f);
                doHurtTarget(target);
                callBrood(level, target);
            }
        }
    }

    /** One strike's worth of helpers, up to the configured brood size. Returns how many came. */
    public int callBrood(ServerLevel level, LivingEntity victim) {
        int limit = DzConfig.QUEEN_MAX_BROOD.get();
        int near = Summons.countNear(this, 32.0, e -> e instanceof BloodlustAnt || isBrood(e));
        int came = 0;
        if (near < limit && this.random.nextInt(3) == 1) {
            double x = (getX() + victim.getX()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
            double y = (getY() + victim.getY()) / 2.0;
            double z = (getZ() + victim.getZ()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
            if (Summons.spawnAt(level, ModContent.BLOODLUST_ANT.get(), x, y, z, victim)) {
                near++;
                came++;
            }
        }
        if (near < limit) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(BROOD.get(this.random.nextInt(BROOD.size())));
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = getBbWidth() / 2.0 + 2.0;
            if (Summons.spawnAt(level, type, getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius, null)) {
                came++;
            }
        }
        return came;
    }

    private static boolean isBrood(Entity entity) {
        return Set.copyOf(BROOD).contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_QUEEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_QUEEN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0f;
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

    /** Straight at the target, or to a nearby spot when there is none. */
    private final class MarchGoal extends Goal {
        private Vec3 wander;
        private int wanderTime;

        MarchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0f, 30.0f);
                if (distanceToSqr(target) > Math.pow(getBbWidth() / 2.0 + 3.0, 2.0)) {
                    getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
                }
                return;
            }
            if (this.wander == null || --this.wanderTime <= 0) {
                this.wanderTime = 80 + getRandom().nextInt(120);
                this.wander = position().add((getRandom().nextDouble() - 0.5) * 32.0, 0.0, (getRandom().nextDouble() - 0.5) * 32.0);
            }
            if (getRandom().nextInt(3) != 0 && !level().getBlockState(blockPosition().below()).is(BlockTags.AIR)) {
                getMoveControl().setWantedPosition(this.wander.x, this.wander.y, this.wander.z, 0.6);
            }
        }
    }
}
