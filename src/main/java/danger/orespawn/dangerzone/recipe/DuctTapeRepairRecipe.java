package danger.orespawn.dangerzone.recipe;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The port's Duct Tape plus any one damaged unstackable item: the item comes back repaired with its enchantments and
 * everything else intact. More tape repairs more when a repair is set below a full one.
 */
public class DuctTapeRepairRecipe extends CustomRecipe {
    private static final ResourceLocation TAPE = ResourceLocation.fromNamespaceAndPath("orespawn", "duct_tape");

    public DuctTapeRepairRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Item tape() {
        return BuiltInRegistries.ITEM.get(TAPE);
    }

    private static ItemStack damaged(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        int tapes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(tape())) {
                tapes++;
            } else if (found.isEmpty() && stack.isDamaged() && stack.getMaxStackSize() == 1 && !stack.is(ModContent.NOT_TAPEABLE)) {
                found = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return tapes > 0 ? found : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !damaged(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack item = damaged(input);
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int tapes = 0;
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(tape())) {
                tapes++;
            }
        }
        ItemStack repaired = item.copy();
        int restored = (int) Math.ceil(repaired.getMaxDamage() * DzConfig.TAPE_REPAIR_FRACTION.get() * tapes);
        repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - restored));
        return repaired;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent.DUCT_TAPE_REPAIR.get();
    }
}
