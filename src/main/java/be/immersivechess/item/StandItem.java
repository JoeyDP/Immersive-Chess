package be.immersivechess.item;

import be.immersivechess.block.StandBlock;
import be.immersivechess.recipe.StandDyeRecipe;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.util.DyeColor;

// Do not extend DyeableItem because this enables color blending recipes for stand.
public class StandItem extends BlockItem {

    public static final DyeColor DEFAULT_COLOR = DyeColor.YELLOW;
    public static final int DEFAULT_COLOR_INT = StandDyeRecipe.dyeColorToColor(DEFAULT_COLOR);

    public StandItem(StandBlock standBlock, Settings settings) {
        super(standBlock, settings);
    }

}
