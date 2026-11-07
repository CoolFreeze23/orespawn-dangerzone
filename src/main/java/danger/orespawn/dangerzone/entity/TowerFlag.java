package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DangerZone's Tower Defense Flag. A ten-block pole that hostile creatures attack. Every 30 seconds it calls the next
 * species on a ladder of every eligible hostile creature, weakest first, and spins and smokes as its health falls.
 * Players can't hurt it. Survive the ladder and it pays out, hands the flag back and comes down.
 */
public class TowerFlag extends Mob {
    private static final ResourceKey<LootTable> REWARD =
            ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("gameplay/tower_flag_reward"));

    private static final int DRIVE_EVERY = 20;
    private static final double STRIKE_REACH = 2.5;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private final List<UUID> attackers = new ArrayList<>();
    private final Map<UUID, Vec3> lastSeen = new HashMap<>();
    private int counter;
    /** The next rung of the ladder, or -1 once it's over. */
    private int next;

    public TowerFlag(EntityType<? extends TowerFlag> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 1;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public boolean finished() {
        return this.next < 0;
    }

    public int rung() {
        return this.next;
    }

    /** Every eligible species, weakest first, by DangerZone's measure of attack times defence. */
    public static List<EntityType<?>> ladder() {
        List<EntityType<?>> types = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(ModContent.TOWER_DEFENSE).forEach(holder -> types.add(holder.value()));
        types.sort(Comparator.<EntityType<?>>comparingDouble(TowerFlag::hardness).thenComparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        return types;
    }

    /** DangerZone's defence divides damage, where armour subtracts: armour is read back to a defence figure. */
    public static double hardness(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return 0.0;
        }
        @SuppressWarnings("unchecked")
        AttributeSupplier attributes = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        double attack = attributes.hasAttribute(Attributes.ATTACK_DAMAGE) ? attributes.getBaseValue(Attributes.ATTACK_DAMAGE) : 1.0;
        double armor = attributes.hasAttribute(Attributes.ARMOR) ? attributes.getBaseValue(Attributes.ARMOR) : 0.0;
        return attack * Math.max(1.0, (armor - 4.0) / 4.0);
    }

    public static int waveSize(double hardness) {
        if (hardness > 500) return 4;
        if (hardness > 100) return 5;
        if (hardness > 30) return 6;
        if (hardness > 10) return 8;
        if (hardness > 5) return 10;
        return 12;
    }

    @Override
    public void tick() {
        super.tick();
        // The spin is its health bar: the lower, the faster.
        float h = Math.max(1.0f, getHealth());
        float pressure = getMaxHealth() / h;
        float spin = Math.min(100.0f, (pressure - 1.0f) * 5.0f);
        this.yBodyRotO = this.yBodyRot;
        this.yBodyRot += spin;
        setYRot(this.yBodyRot);
        this.yHeadRot = this.yBodyRot;
        this.yRotO = this.yBodyRotO;
        this.yHeadRotO = this.yBodyRotO;
        setDeltaMovement(0.0, 0.0, 0.0);

        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // A pole still standing from before it came down by itself: its flag has already been handed back.
        if (finished()) {
            discard();
            return;
        }
        removeAllEffects();
        float effects = Math.min(32.0f, pressure);
        if (effects > 4.0f) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + this.random.nextFloat() * getBbHeight(), getZ(), (int) (effects / 2.0f), 0.2, 0.2, 0.2, 0.01);
        }
        if (effects > 2.0f) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + this.random.nextFloat() * getBbHeight(), getZ(), (int) (effects * 2.0f), 0.2, 0.2, 0.2, 0.01);
        }
        this.bossBar.setProgress(h / getMaxHealth());
        clearAround(level);
        if (this.tickCount % DRIVE_EVERY == 0) {
            driveAttackers(level);
        }
        if (++this.counter > DzConfig.FLAG_WAVE_SECONDS.get() * 20) {
            this.counter = 0;
            nextWave(level);
        }
    }

    /** Keeps the area clear, so the flag can't be walled in. Follows the mobGriefing rule and leaves containers alone. */
    private void clearAround(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || !DzConfig.FLAG_CLEARS_BLOCKS.get()) {
            return;
        }
        for (int n = 0; n < 10; n++) {
            BlockPos pos = blockPosition().offset(this.random.nextInt(5) - this.random.nextInt(5), this.random.nextInt(15), this.random.nextInt(5) - this.random.nextInt(5));
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0f) {
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }

    /**
     * Brings the wave in. DangerZone's creatures all share one engine's attack logic; the port's have their own, and
     * many don't chase whatever their target slot holds. So the flag walks its attackers in, shoves any that aren't
     * getting anywhere, and has them strike it with their own attack. One that has found a player fights the player.
     */
    private void driveAttackers(ServerLevel level) {
        this.attackers.removeIf(id -> {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                this.lastSeen.remove(id);
                return true;
            }
            LivingEntity target = mob.getTarget();
            if (target instanceof Player && target.isAlive()) {
                return false;
            }
            if (target != this) {
                mob.setTarget(this);
            }
            double reach = STRIKE_REACH + mob.getBbWidth() / 2.0 + getBbWidth() / 2.0;
            Vec3 flat = new Vec3(getX() - mob.getX(), 0.0, getZ() - mob.getZ());
            if (flat.length() <= reach) {
                strike(level, mob);
                return false;
            }
            mob.getNavigation().moveTo(getX(), getY(), getZ(), 1.2);
            Vec3 before = this.lastSeen.put(id, mob.position());
            if (before != null && before.distanceTo(mob.position()) < 0.5) {
                Vec3 shove = flat.normalize().scale(0.4);
                mob.setDeltaMovement(shove.x, Math.max(mob.getDeltaMovement().y, mob.horizontalCollision ? 0.42 : 0.0), shove.z);
                mob.hasImpulse = true;
                float yaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
                mob.setYRot(yaw);
                mob.setYBodyRot(yaw);
            }
            return false;
        });
    }

    private void strike(ServerLevel level, Mob mob) {
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            hurt(damageSources().mobAttack(mob), 1.0f);
        } else {
            mob.doHurtTarget(this);
        }
    }

    /** Adds one creature to the wave. Public for the tests. */
    public void addAttacker(Mob mob) {
        mob.setTarget(this);
        this.attackers.add(mob.getUUID());
    }

    /** Calls the next species, or ends the event. Public for the tests. */
    public void nextWave(ServerLevel level) {
        List<EntityType<?>> ladder = ladder();
        if (this.next >= ladder.size()) {
            finish(level);
            return;
        }
        EntityType<?> type = ladder.get(this.next++);
        int howMany = waveSize(hardness(type));
        for (int i = 0; i < howMany; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 20.0 + type.getWidth();
            int x = (int) Math.floor(getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(getZ() + Math.sin(angle) * radius);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Entity entity = type.create(level);
            if (entity instanceof Mob mob) {
                mob.moveTo(x + 0.5, y, z + 0.5, this.random.nextFloat() * 360.0f, 0.0f);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                mob.setTarget(this);
                if (level.addFreshEntity(mob)) {
                    this.attackers.add(mob.getUUID());
                }
            }
        }
        bolt(level);
        this.bossBar.setName(Component.translatable("entity.orespawn_dangerzone.tower_flag.wave", this.next, ladder.size(), type.getDescription()));
    }

    private void bolt(ServerLevel level) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(getX(), getY(), getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private void finish(ServerLevel level) {
        this.next = -1;
        setHealth(getMaxHealth());
        this.bossBar.setName(getDisplayName());
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        level.getServer().reloadableRegistries().getLootTable(REWARD).getRandomItems(params).forEach(this::spawnAtLocation);
        spawnAtLocation(new ItemStack(ModContent.TOWER_FLAG_ITEM.get()));
        bolt(level);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        if (source.getEntity() instanceof Player && !source.isCreativePlayer()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Counter", this.counter);
        tag.putInt("Next", this.next);
        StringBuilder ids = new StringBuilder();
        this.attackers.forEach(id -> ids.append(id).append(','));
        tag.putString("Attackers", ids.toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.counter = tag.getInt("Counter");
        this.next = tag.getInt("Next");
        this.attackers.clear();
        for (String id : tag.getString("Attackers").split(",")) {
            if (!id.isEmpty()) {
                this.attackers.add(UUID.fromString(id));
            }
        }
    }
}
