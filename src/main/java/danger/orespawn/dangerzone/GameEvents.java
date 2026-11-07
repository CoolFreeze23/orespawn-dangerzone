package danger.orespawn.dangerzone;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class GameEvents {
    private static final float EYE_CHANCE = 0.25f;

    private GameEvents() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        // A DangerZone baby leaves nothing behind, so a shrunk creature can't be farmed.
        if (entity.hasEffect(ModContent.SHRUNK)) {
            event.getDrops().clear();
            return;
        }
        if (entity.getType().is(ModContent.DROPS_DRAGONS_EYE) && entity.getRandom().nextFloat() < EYE_CHANCE) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    new ItemStack(ModContent.DRAGONS_EYE.get())));
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().hasEffect(ModContent.SHRUNK)) {
            event.setDroppedExperience(1);
        }
    }
}
