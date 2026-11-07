package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.morph.Morph;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Use it on a creature to become one. Use it on nothing to go back. A Dragon's Eye recharges it at an anvil. */
public class MorphStaffItem extends Item {
    public MorphStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!Morph.canBecome(target)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            Morph.set(server, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
            // A fresh copy: the stack handed in here isn't always the one still in the hand.
            player.getItemInHand(hand).hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            Morph.set(server, "");
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModContent.DRAGONS_EYE.get());
    }
}
