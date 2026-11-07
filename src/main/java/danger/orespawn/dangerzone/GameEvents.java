package danger.orespawn.dangerzone;

import danger.orespawn.dangerzone.entity.PrincessGrowth;
import danger.orespawn.dangerzone.item.TrophyItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
        // A player's drops are their inventory, which is another matter.
        if (entity.hasEffect(ModContent.SHRUNK) && !(entity instanceof Player)) {
            event.getDrops().clear();
            return;
        }
        if (entity.getType().is(ModContent.DROPS_DRAGONS_EYE) && entity.getRandom().nextFloat() < EYE_CHANCE) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    new ItemStack(ModContent.DRAGONS_EYE.get())));
        }
        if (entity instanceof Mob && !entity.getType().is(ModContent.NO_TROPHY)) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (id.getNamespace().equals("orespawn") || id.getNamespace().equals(OreSpawnDangerZone.MOD_ID)) {
                double chance = entity.getType().is(ModContent.SHRINK_IMMUNE) ? DzConfig.BOSS_TROPHY_CHANCE.get() : DzConfig.TROPHY_CHANCE.get();
                if (entity.getRandom().nextDouble() < chance) {
                    event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), TrophyItem.of(id.toString())));
                }
            }
        }
    }

    /** A diamond block grows the owner's baby Princess into a Teen, as the Teen's does into a Young Adult. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof TamableAnimal princess && princess.getType() == PrincessGrowth.baby()
                && princess.isTame() && princess.isOwnedBy(event.getEntity())
                && event.getItemStack().is(Blocks.DIAMOND_BLOCK.asItem())) {
            InteractionResult result = PrincessGrowth.become(princess, ModContent.PRINCESS_TEEN.get(), event.getEntity(), event.getItemStack());
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().hasEffect(ModContent.SHRUNK) && !(event.getEntity() instanceof Player)) {
            event.setDroppedExperience(1);
        }
    }
}
