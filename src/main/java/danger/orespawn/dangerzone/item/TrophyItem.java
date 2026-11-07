package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.Trophy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One item for every creature, which remembers its creature; DangerZone registers one per species. */
public class TrophyItem extends Item {
    private static final String KEY = "orespawn_dangerzone_trophy";

    public TrophyItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String entityId) {
        ItemStack stack = new ItemStack(ModContent.TROPHY.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY, entityId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String subject(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(KEY);
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = subject(stack);
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null && BuiltInRegistries.ENTITY_TYPE.containsKey(location)) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(location);
            return Component.translatable("item.orespawn_dangerzone.trophy.of", type.getDescription());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        String id = subject(stack);
        if (id.isEmpty() || context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos().above();
            Trophy trophy = ModContent.TROPHY_ENTITY.get().create(level);
            if (trophy == null) {
                return InteractionResult.FAIL;
            }
            Vec3 feet = Vec3.atBottomCenterOf(pos);
            trophy.moveTo(feet.x, feet.y, feet.z, 0.0f, 0.0f);
            trophy.setSubject(id);
            level.addFreshEntity(trophy);
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
