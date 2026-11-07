package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A hundred segments of worm that swims through the ground on a sine wave, carving as it goes, and heads for the
 * nearest player. Its bite can tear off armour and whatever you're holding. Worm Food in your hand and it leaves you
 * be. It can't pass bedrock or obsidian: hit either and it blows up along its length.
 */
public class DoomWorm extends Monster implements DzAnimated {
    public static final int SEGMENTS = 100;
    private static final double SEGMENT_GAP = 1.5;
    private static final double SEARCH = 128.0;
    private static final double REACH = 6.0;
    private static final int CARVE_RADIUS = 3;

    public final double[] trailX = new double[SEGMENTS];
    public final double[] trailY = new double[SEGMENTS];
    public final double[] trailZ = new double[SEGMENTS];

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private boolean trailReady;
    private Vec3 heading = new Vec3(1.0, 0.0, 0.0);
    private int wave;
    private int bites;
    private int backoff;
    private boolean exploding;

    public DoomWorm(EntityType<? extends DoomWorm> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        setNoGravity(true);
        this.xpReward = 5000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.ATTACK_DAMAGE, 200.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.FOLLOW_RANGE, SEARCH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Holding Worm Food, or one of its own eggs, buys your way past it. */
    public static boolean ignores(Player player) {
        return player.isHolding(ModContent.WORM_FOOD.get()) || player.isHolding(ModContent.DOOM_WORM_EGG.get());
    }

    @Override
    public void tick() {
        super.tick();
        updateTrail();
        if (isNoAi() || isDeadOrDying() || !(level() instanceof ServerLevel level)) {
            return;
        }
        swim(level);
        this.bossBar.setProgress(getHealth() / getMaxHealth());
    }

    /** Each segment takes the place of the one ahead once the head has moved a segment's length. */
    private void updateTrail() {
        if (!this.trailReady) {
            this.trailReady = true;
            for (int i = 0; i < SEGMENTS; i++) {
                this.trailX[i] = getX() - this.heading.x * i * SEGMENT_GAP;
                this.trailY[i] = getY();
                this.trailZ[i] = getZ() - this.heading.z * i * SEGMENT_GAP;
            }
        }
        double dx = getX() - this.trailX[0];
        double dy = getY() - this.trailY[0];
        double dz = getZ() - this.trailZ[0];
        if (dx * dx + dy * dy + dz * dz >= SEGMENT_GAP * SEGMENT_GAP) {
            System.arraycopy(this.trailX, 0, this.trailX, 1, SEGMENTS - 1);
            System.arraycopy(this.trailY, 0, this.trailY, 1, SEGMENTS - 1);
            System.arraycopy(this.trailZ, 0, this.trailZ, 1, SEGMENTS - 1);
            this.trailX[0] = getX();
            this.trailY[0] = getY();
            this.trailZ[0] = getZ();
        }
    }

    private void swim(ServerLevel level) {
        Player target = level.getNearestPlayer(getX(), getY(), getZ(), SEARCH, p -> !((Player) p).isCreative() && !p.isSpectator() && !ignores((Player) p));
        setAggressive(target != null);
        if (target != null && this.random.nextInt(20) == 0) {
            Vec3 toward = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (toward.lengthSqr() > 1.0) {
                this.heading = toward.normalize();
            }
        } else if (target == null && this.random.nextInt(200) == 0) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            this.heading = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        // Up and down through the ground on a sine wave, around the surface, or at what it's hunting.
        this.wave++;
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
        double middle = target != null && distanceToSqr(target) < 400.0 ? target.getY() : surface - 3.0;
        double wantY = middle + Math.sin(this.wave * 0.05) * 6.0;
        double speed = target != null ? 0.45 : 0.25;
        setDeltaMovement(this.heading.x * speed, Math.max(-0.4, Math.min(0.4, (wantY - getY()) * 0.1)), this.heading.z * speed);
        this.hasImpulse = true;
        float yaw = (float) Math.toDegrees(Math.atan2(-this.heading.x, this.heading.z));
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            carve(level);
        }
        if (target != null) {
            if (this.backoff > 0) {
                this.backoff--;
            } else if (distanceToSqr(target) < (REACH + getBbWidth() / 2.0) * (REACH + getBbWidth() / 2.0) && this.random.nextInt(4) == 0) {
                bite(target);
            }
        }
    }

    /** A tunnel around the head. A block the worm can't pass ends it, in the DangerZone manner. Public for the tests. */
    public void carve(ServerLevel level) {
        BlockPos centre = blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -CARVE_RADIUS; dx <= CARVE_RADIUS; dx++) {
            for (int dy = -CARVE_RADIUS; dy <= CARVE_RADIUS; dy++) {
                for (int dz = -CARVE_RADIUS; dz <= CARVE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > CARVE_RADIUS * CARVE_RADIUS) {
                        continue;
                    }
                    pos.setWithOffset(centre, dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.REINFORCED_DEEPSLATE)) {
                        blowUp(level);
                        return;
                    }
                    if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0f) {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    if (this.random.nextInt(10) == 0) {
                        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(state.getBlock()));
                        item.setPickUpDelay(10);
                        item.lifespan = 200;
                        level.addFreshEntity(item);
                    }
                }
            }
        }
    }

    /**
     * Every ninth segment goes off, then the worm is gone, leaving nothing: killing it by leading it to obsidian is no
     * way to get its loot. DangerZone's every third, at power 15, levels a county.
     */
    private void blowUp(ServerLevel level) {
        if (this.exploding) {
            return;
        }
        this.exploding = true;
        for (int i = 0; i < SEGMENTS; i += 9) {
            level.explode(this, this.trailX[i], this.trailY[i], this.trailZ[i], 5.0f, false, Level.ExplosionInteraction.MOB);
        }
        discard();
    }

    /** One bite. It may strip each armour slot and the held item, spitting them out. Public for the tests. */
    public void bite(LivingEntity victim) {
        if (doHurtTarget(victim) && !level().isClientSide) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if ((slot.isArmor() || slot == EquipmentSlot.MAINHAND) && this.random.nextInt(4) == 0) {
                    strip(victim, slot);
                }
            }
        }
        if (++this.bites >= 20) {
            this.bites = 0;
            this.backoff = 100;
        }
    }

    /** Public for the tests. */
    public static void strip(LivingEntity victim, EquipmentSlot slot) {
        ItemStack stack = victim.getItemBySlot(slot);
        if (!stack.isEmpty()) {
            victim.spawnAtLocation(stack.copy());
            victim.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Being underground isn't its problem.
        return !source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) && super.hurt(source, amount);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(SEGMENTS * SEGMENT_GAP);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_WORM_AMBIENT.get();
    }

    @Override
    public float getNewangle() {
        return 0.0f;
    }

    @Override
    public boolean dzAttacking() {
        return isAggressive();
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
