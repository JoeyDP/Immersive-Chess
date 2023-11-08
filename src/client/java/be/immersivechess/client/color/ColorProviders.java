package be.immersivechess.client.color;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.PieceStandBlock;
import be.immersivechess.block.entity.DyedStructureRenderedBlockEntity;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.PieceStandItem;
import be.immersivechess.item.StandItem;
import be.immersivechess.recipe.StandDyeRecipe;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public class ColorProviders {

    /**
     * Forwards colorProviders based on tintIndex mapping int TintMapper
     */
    private static class ForwardingColorProvider implements BlockColorProvider, ItemColorProvider{
        @Override
        public int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
            BlockColorProvider colorProvider = TintMapper.INSTANCE.getBlockColorProvider(tintIndex);
            return colorProvider == null ? -1 : colorProvider.getColor(net.minecraft.block.Blocks.AIR.getDefaultState(), world, pos, tintIndex % TintMapper.CAPACITY);
        }

        @Override
        public int getColor(ItemStack stack, int tintIndex) {
            // We only need to forward colors for blocks, not potions etc.
            return getColor(net.minecraft.block.Blocks.AIR.getDefaultState(), null, null, tintIndex);
        }
    }

    private static class DyeColorProvider implements BlockColorProvider, ItemColorProvider{
        // create dummy to access non-static functions that could have been static.
        private static final DyeableItem DYEABLE_ITEM = new DyeableItem(){};

        @Override
        public int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
            if (tintIndex > 0 || world == null || pos == null)
                return -1;

            if (world.getBlockEntity(pos) instanceof DyedStructureRenderedBlockEntity blockEntity)
                return blockEntity.getColor();

            return StandItem.DEFAULT_COLOR_INT;
        }

        @Override
        public int getColor(ItemStack stack, int tintIndex) {
            if (tintIndex > 0)
                return -1;

            return PieceContainer.getColor(stack);
        }
    }

    /**
     * Combines ForwardingColorProvider and DyeColorProvider based on tintIndex
     */
    private static class CombinedColorProvider implements BlockColorProvider, ItemColorProvider{
        private final DyeColorProvider dyeColorProvider;
        private final ForwardingColorProvider forwardingColorProvider;

        public CombinedColorProvider(DyeColorProvider dyeColorProvider, ForwardingColorProvider forwardingColorProvider){
            this.dyeColorProvider = dyeColorProvider;
            this.forwardingColorProvider = forwardingColorProvider;
        }

        @Override
        public int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
            return tintIndex < TintMapper.CAPACITY ? dyeColorProvider.getColor(state, world, pos, tintIndex) : forwardingColorProvider.getColor(state, world, pos, tintIndex);
        }

        @Override
        public int getColor(ItemStack stack, int tintIndex) {
            return tintIndex < TintMapper.CAPACITY ? dyeColorProvider.getColor(stack, tintIndex) : forwardingColorProvider.getColor(stack, tintIndex);
        }
    }

    public static void onInitializeClient() {
        ForwardingColorProvider forwardingColorProvider = new ForwardingColorProvider();
        DyeColorProvider dyeColorProvider = new DyeColorProvider();
        CombinedColorProvider combinedColorProvider = new CombinedColorProvider(dyeColorProvider, forwardingColorProvider);

        // structure rendered blocks and items
        ColorProviderRegistry.BLOCK.register(forwardingColorProvider, Blocks.BOARD_BLOCK);
        ColorProviderRegistry.BLOCK.register(forwardingColorProvider, Blocks.PIECES.values().toArray(new Block[0]));
        ColorProviderRegistry.ITEM.register(forwardingColorProvider, Items.PIECE_ITEMS.values().toArray(new Item[0]));

        // stands that can be dyed in blocks and items
        ColorProviderRegistry.BLOCK.register(dyeColorProvider, Blocks.BLANK_STAND);
        ColorProviderRegistry.ITEM.register(dyeColorProvider, Items.BLANK_STAND);

        // blocks and items that can both be dyed and contain a structure
        ColorProviderRegistry.BLOCK.register(combinedColorProvider, Blocks.PIECE_STANDS.toArray(new Block[0]));
        ColorProviderRegistry.BLOCK.register(combinedColorProvider, Blocks.PIECE_STRUCTURE_BLOCKS.toArray(new Block[0]));
        ColorProviderRegistry.ITEM.register(combinedColorProvider, Items.PIECE_STANDS.values().toArray(new PieceStandItem[0]));
    }


}
