package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.item.TrophyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * A tiny spinning model of a creature, standing on a block. Hit it or use it to pick it up. An explosion knocks it
 * loose, it falls off when its block goes, and two in one spot push each other off, just to see them pile up.
 */
public class Trophy extends Entity {
    private static final EntityDataAccessor<String> SUBJECT = SynchedEntityData.defineId(Trophy.class, EntityDataSerializers.STRING);

    private int checker;

    public Trophy(EntityType<? extends Trophy> type, Level level) {
        super(type, level);
        this.checker = 100 + this.random.nextInt(100);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SUBJECT, "");
    }

    public String subject() {
        return this.entityData.get(SUBJECT);
    }

    public void setSubject(String id) {
        this.entityData.set(SUBJECT, id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        BlockPos below = blockPosition().below();
        if (level().getBlockState(below).is(Blocks.AIR)) {
            popOff();
            return;
        }
        if (--this.checker <= 0) {
            this.checker = 100 + this.random.nextInt(100);
            List<Trophy> near = level().getEntitiesOfClass(Trophy.class, getBoundingBox().inflate(1.0),
                    other -> other != this && other.blockPosition().equals(blockPosition()));
            if (!near.isEmpty()) {
                popOff();
            }
        }
    }

    /** Back to an item. Public for the tests. */
    public void popOff() {
        if (!isRemoved()) {
            spawnAtLocation(TrophyItem.of(subject()));
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && (source.is(DamageTypeTags.IS_EXPLOSION) || source.getEntity() instanceof Player)) {
            popOff();
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            popOff();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSubject(tag.getString("Subject"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Subject", subject());
    }
}
