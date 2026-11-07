package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

/** Moving the Princess between her stages. The owner, her name and her health fraction go with her. */
public final class PrincessGrowth {
    private PrincessGrowth() {
    }

    @SuppressWarnings("unchecked")
    public static EntityType<? extends TamableAnimal> baby() {
        return (EntityType<? extends TamableAnimal>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("orespawn", "the_princess"));
    }

    public static InteractionResult become(TamableAnimal from, EntityType<? extends TamableAnimal> to, Player player, ItemStack cost) {
        if (!(from.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        TamableAnimal next = to.create(level);
        if (next == null) {
            return InteractionResult.FAIL;
        }
        next.moveTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        next.tame(player);
        next.setPersistenceRequired();
        if (from.hasCustomName()) {
            next.setCustomName(from.getCustomName());
        }
        next.setHealth(next.getMaxHealth() * Math.max(0.05f, from.getHealth() / from.getMaxHealth()));
        level.addFreshEntity(next);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, from.getX(), from.getY() + from.getBbHeight() / 2.0, from.getZ(), 20, 1.0, 1.0, 1.0, 0.0);
        level.playSound(null, from.blockPosition(), SoundEvents.CHICKEN_EGG, from.getSoundSource(), 1.0f, 1.0f);
        from.discard();
        cost.consume(1, player);
        return InteractionResult.CONSUME;
    }

    static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("This OreSpawn has no " + owner.getSimpleName() + "." + name, e);
        }
    }

    static void zero(Field field, Object target) {
        try {
            field.setInt(target, 0);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
