package be.immersivechess.block;

import be.immersivechess.block.entity.BlockEntityTypes;
import be.immersivechess.logic.Piece;
import be.immersivechess.structure.StructureOutlines;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class PieceStandBlock extends StandBlock {

    private final Piece piece;

    private final VoxelShape outlineShape;

    // Blockstate properties
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public PieceStandBlock(Settings settings, Piece piece) {
        super(settings);
        this.piece = piece;
        outlineShape = VoxelShapes.union(
                StandBlock.OUTLINE_SHAPE,
                Block.createCuboidShape(4, 0, 4, 12, 3 + piece.getHeight(), 12)
        );
        setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public Piece getPiece() {
        return piece;
    }

//    @Override
//    public BlockRenderType getRenderType(BlockState state) {
//        return BlockRenderType.MODEL;
//    }
//
//    // from BlockWithEntity
//    @Override
//    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
//        super.onSyncedBlockEvent(state, world, pos, type, data);
//        BlockEntity blockEntity = world.getBlockEntity(pos);
//        if (blockEntity == null) {
//            return false;
//        }
//        return blockEntity.onSyncedBlockEvent(type, data);
//    }
//
//    @Override
//    public PieceStandBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
//        return new PieceStandBlockEntity(pos, state);
//    }
//
//    @Override
//    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
//        ItemStack itemStack = super.getPickStack(world, pos, state);
//        world.getBlockEntity(pos, BlockEntityTypes.PIECE_STAND_BLOCK_ENTITY_TYPE).ifPresent(blockEntity -> {
//            blockEntity.setStackNbt(itemStack);
//            // setStackNbt also sets the id of the entity which is not needed and causes duplication. Remove it.
//            if (itemStack.hasNbt() && itemStack.getNbt().contains(BlockItem.BLOCK_ENTITY_TAG_KEY))
//                itemStack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY).remove("id");
//        });
//        return itemStack;
//    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // stands should face the player
        Direction playerFacing = ctx.getHorizontalPlayerFacing();
        Direction direction = ctx.shouldCancelInteraction() ? playerFacing : playerFacing.getOpposite();
        return getDefaultState().with(FACING, direction);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return world.getBlockEntity(pos, BlockEntityTypes.PIECE_STAND_BLOCK_ENTITY_TYPE)
                .map(be -> StructureOutlines.getOrCreateOutline(be.getStructure(), state.get(FACING)).orElse(outlineShape))
                .map(shape -> StructureOutlines.scale(shape, 0.5f).offset(0.25, 3f / 16f, 0.25))
                .map(shape -> VoxelShapes.combine(StandBlock.OUTLINE_SHAPE, shape, BooleanBiFunction.OR))
                .orElse(outlineShape);
    }
}
