package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One of six dragons, one for each element: earth, wind, fire, water, lightning, plant. It circles what it guards and
 * attacks with its element, and it drops the staff of that element, which is why you came.
 */
public class ElementDragon extends DzFlyer implements DzAnimated {
    public static final int ELEMENTS = 6;
    private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementDragon.class, EntityDataSerializers.INT);
    private static final int[] SHOTS = {4, 6, 4, 4, 3, 4};

    private int reload;

    public ElementDragon(EntityType<? extends ElementDragon> type, Level level) {
        super(type, level);
        this.xpReward = 2000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DzFlyer.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 38.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.FOLLOW_RANGE, 100.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, 0);
    }

    public int element() {
        return this.entityData.get(ELEMENT);
    }

    public void setElement(int element) {
        this.entityData.set(ELEMENT, Math.floorMod(element, ELEMENTS));
    }

    @Override
    protected double speed() {
        return 0.5;
    }

    @Override
    protected double search() {
        return 64.0;
    }

    @Override
    protected double holdOff() {
        return 10.0;
    }

    @Override
    protected void attack(ServerLevel level, LivingEntity target, double distance) {
        if (--this.reload > 0) {
            return;
        }
        this.reload = 40 + this.random.nextInt(30);
        if (distance < 7.0) {
            doHurtTarget(target);
        }
        if (distance < 48.0 && hasLineOfSight(target)) {
            int shots = 1 + this.random.nextInt(SHOTS[element()]);
            for (int i = 0; i < shots; i++) {
                ElementShot shot = new ElementShot(level, this, element());
                Vec3 aim = target.getEyePosition().subtract(position());
                shot.setPos(getX(), getY(0.6), getZ());
                shot.shoot(aim.x, aim.y, aim.z, 1.2f, 4.0f);
                level.addFreshEntity(shot);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        List<? extends Item> staffs = ModContent.elementStaffs();
        spawnAtLocation(staffs.get(element()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Element", element());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setElement(tag.getInt("Element"));
    }

    @Override
    public boolean dzAttacking() {
        return isAggressive();
    }

    @Override
    public boolean dzFlying() {
        return true;
    }

    @Override
    public float getNewangle() {
        return 0.0f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_ELEMENT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_ELEMENT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_ELEMENT_DEATH.get();
    }
}
