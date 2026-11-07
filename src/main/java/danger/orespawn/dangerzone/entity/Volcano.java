package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible point that builds its own mountain. Its flow pulses up and down, throwing burning rock, lava, the odd
 * diamond ore and now and then live explosives; it climbs as the cone grows around it and dies out at height 200.
 * Throws nothing while mobGriefing is off.
 */
public class Volcano extends Mob {
    private static final int TOP = 200;

    private int flowRate = 50;
    private int flowCount;
    private int flowDir = 1;

    public Volcano(EntityType<? extends Volcano> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noPhysics = true;
        setInvisible(true);
        setSilent(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public static boolean checkVolcanoSpawnRules(EntityType<Volcano> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return random.nextInt(4) == 0 && level.getEntitiesOfClass(Volcano.class, new net.minecraft.world.phys.AABB(pos).inflate(256.0)).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int flow = Math.max(2, this.flowRate - this.flowCount);
        this.flowCount += this.flowDir;
        if (this.flowCount > this.flowRate) {
            this.flowDir = -1;
        }
        if (this.flowDir < 0 && this.flowCount <= 0) {
            this.flowRate = this.random.nextInt(100);
            this.flowDir = 1;
            this.flowCount = 1;
        }
        if (this.random.nextInt(flow * 2) == 1) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0, getZ(), 3 + this.random.nextInt(9), 0.5, 0.2, 0.5, 0.0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 6, 0.5, 0.5, 0.5, 0.03);
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        spoutFrom(level, flow);
        if (this.tickCount % 20 == 0) {
            climb(level);
        }
    }

    /** One tick of the flow. Public for the tests. */
    public int spoutFrom(ServerLevel level, int flow) {
        int count = Summons.countNear(this, 96.0, e -> e instanceof FallingBlockEntity);
        if (count >= DzConfig.VOLCANO_MAX_FLYING.get()) {
            return 0;
        }
        float height = 1.75f + this.random.nextFloat() * 0.5f;
        int thrown = 0;
        // DangerZone's lava filler and lava: magma stands in where a falling lava block would be invisible in flight.
        thrown += roll(flow) ? throwBlock(level, Blocks.MAGMA_BLOCK.defaultBlockState(), height) : 0;
        thrown += roll(flow) ? throwBlock(level, Blocks.LAVA.defaultBlockState(), height) : 0;
        thrown += roll(flow) ? throwBlock(level, Blocks.STONE.defaultBlockState(), height) : 0;
        thrown += roll(flow) ? throwBlock(level, Blocks.ANDESITE.defaultBlockState(), height) : 0;
        thrown += roll(flow * 4) ? throwBlock(level, Blocks.DIAMOND_ORE.defaultBlockState(), height / 2.0f) : 0;
        if (roll(flow * 4)) {
            PrimedTnt tnt = new PrimedTnt(level, getX(), getY() + 1.25, getZ(), null);
            tnt.setDeltaMovement(launch(height));
            tnt.setFuse(40 + this.random.nextInt(60));
            level.addFreshEntity(tnt);
            thrown++;
        }
        return thrown;
    }

    private boolean roll(int bound) {
        return this.random.nextInt(bound) == 1;
    }

    private Vec3 launch(float height) {
        // DangerZone's speeds are per second at a 0.05 tick; Minecraft's are per tick.
        double up = (height + this.random.nextFloat() * height) * 0.25;
        double x = (this.random.nextFloat() - this.random.nextFloat()) * height * 0.25;
        double z = (this.random.nextFloat() - this.random.nextFloat()) * height * 0.25;
        return new Vec3(x, up, z);
    }

    private int throwBlock(ServerLevel level, BlockState state, float height) {
        BlockPos pos = BlockPos.containing(getX(), getY() + 1.25, getZ());
        if (!level.getBlockState(pos).isAir()) {
            return 0;
        }
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.dropItem = false;
        block.setDeltaMovement(launch(height));
        block.setHurtsEntities(2.0f, 40);
        if (this.random.nextBoolean()) {
            block.setRemainingFireTicks(this.random.nextInt(30) * 20);
        }
        return 1;
    }

    /** Up one when the cone has built up around it; down one when the ground has fallen away. */
    private void climb(ServerLevel level) {
        BlockPos at = blockPosition();
        int solid = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.getBlockState(at.offset(dx, 0, dz)).isAir()) {
                    solid++;
                }
            }
        }
        if (solid > 0) {
            setPos(getX(), getY() + 1.0, getZ());
        } else if (level.getBlockState(at.below()).isAir()) {
            setPos(getX(), getY() - 1.0, getZ());
        }
        if (getY() > TOP) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Nothing but /kill puts one out.
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
