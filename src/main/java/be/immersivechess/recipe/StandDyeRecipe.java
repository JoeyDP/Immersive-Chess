package be.immersivechess.recipe;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.StandItem;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;

import java.util.Optional;

public class StandDyeRecipe extends SpecialCraftingRecipe {
    public StandDyeRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        return parseIngredients(inventory).isPresent();
    }

    /**
     * Return ItemStack of StandItem to be dyed and single dyeitem if recipe is valid.
     */
    private Optional<Pair<ItemStack, DyeItem>> parseIngredients(RecipeInputInventory inventory) {
        // check whether grid contains exactly one stand and one dye
        ItemStack standItemStack = ItemStack.EMPTY;
        DyeItem dyeItem = null;
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty())
                continue;
            if (itemStack.getItem() instanceof StandItem) {
                if (!standItemStack.isEmpty())
                    return Optional.empty();

                standItemStack = itemStack;
                continue;
            }
            if (itemStack.getItem() instanceof DyeItem inventoryDyeItem) {
                if (dyeItem != null)
                    return Optional.empty();

                dyeItem = inventoryDyeItem;
                continue;
            }

            return Optional.empty();
        }
        if (standItemStack.isEmpty() || dyeItem == null)
            return Optional.empty();

        return Optional.of(new Pair<>(standItemStack, dyeItem));
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        Optional<Pair<ItemStack, DyeItem>> parsedIngredients = parseIngredients(inventory);
        if (parsedIngredients.isEmpty())
            return ItemStack.EMPTY;

        Pair<ItemStack, DyeItem> ingredients = parsedIngredients.get();

        ItemStack result = ingredients.getLeft().copyWithCount(1);
        DyeColor dye = ingredients.getRight().getColor();
        int color = dyeColorToColor(dye);

        if (dye.equals(StandItem.DEFAULT_COLOR))
            PieceContainer.removeColor(result);
        else
            PieceContainer.writeColor(result, color);

        return result;
    }

    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    public static int dyeColorToColor(DyeColor dyeColor) {
        float[] colComp = dyeColor.getColorComponents();
        return ColorHelper.Argb.getArgb(255, (int) (colComp[0] * 255), (int) (colComp[1] * 255), (int) (colComp[2] * 255));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.STAND_DYE;
    }
}
