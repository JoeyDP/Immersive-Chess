package be.immersivechess.block;

import be.immersivechess.block.entity.BlockEntityTypes;
import be.immersivechess.block.entity.PieceStandBlockEntity;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class StandBlock extends BlockWithEntity implements PieceContainer {

    public static final VoxelShape OUTLINE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(3, 0, 4, 13, 3, 12),
            Block.createCuboidShape(4, 0, 3, 12, 3, 13),
            Block.createCuboidShape(2, 0, 6, 14, 2, 10),
            Block.createCuboidShape(6, 0, 2, 10, 2, 14)
    );

    public StandBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PieceStandBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public Piece getPiece() {
        return null;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state);
        world.getBlockEntity(pos, BlockEntityTypes.PIECE_STAND_BLOCK_ENTITY_TYPE).ifPresent(blockEntity -> {
            blockEntity.setStackNbt(itemStack);
            // setStackNbt also sets the id of the entity which is not needed and causes duplication. Remove it.
            if (itemStack.hasNbt() && itemStack.getNbt().contains(BlockItem.BLOCK_ENTITY_TAG_KEY))
                itemStack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY).remove("id");
        });
        return itemStack;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }


}
