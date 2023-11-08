package be.immersivechess.block;

import be.immersivechess.block.entity.PieceStructureBlockEntity;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class PieceStructureBlock extends BlockWithEntity implements PieceContainer {

    public static final int PIECE_BASE_SIZE = 8;
    private static final VoxelShape EMPTY_COLLISION_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 15, 16);
    private final VoxelShape filledCollisionShape;

    private final Piece piece;

    // Blockstate properties
    private static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final BooleanProperty EMPTY = BooleanProperty.of("empty");

    public PieceStructureBlock(Settings settings, Piece piece) {
        super(settings);
        this.piece = piece;
        filledCollisionShape = VoxelShapes.union(
                EMPTY_COLLISION_SHAPE,
                Block.createCuboidShape(3, 13, 3, 13, 16, 13),                      // stand
                Block.createCuboidShape(4, 16, 4, 12, 16 + piece.getHeight(), 12)   // piece
        );
        setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EMPTY, true));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        if (blockEntity instanceof PieceStructureBlockEntity structureBlockEntity) {
            return structureBlockEntity.getEventListener();
        }
        return null;
    }

    /**
     * Called from PieceStructureBlockEntity when the structure needs to be updated.
     */
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(EMPTY))
            return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PieceStructureBlockEntity structureBlockEntity)
            structureBlockEntity.updateStructure();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof PieceStructureBlockEntity structureBlockEntity))
            return ActionResult.FAIL;

        if (state.get(EMPTY)) {
            ItemStack playerHolding = player.getStackInHand(hand);
            if (!playerHolding.getItem().equals(Items.BLANK_STAND)){
                return ActionResult.CONSUME;
            }

            // insert
            if (!world.isClient) {
                ItemStack toInsert = playerHolding.copyWithCount(1);
                if (!player.isCreative())
                    playerHolding.decrement(1);
                structureBlockEntity.setContent(toInsert);
                structureBlockEntity.updateStructure();
                world.setBlockState(pos, state.with(EMPTY, false));
            }
        } else {
            // extract
            if (!world.isClient) {
                player.getInventory().offerOrDrop(structureBlockEntity.clearContent());
                world.setBlockState(pos, state.with(EMPTY, true));
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PieceStructureBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (placer != null && world.getBlockEntity(pos) instanceof PieceStructureBlockEntity pieceStructureBlockEntity)
            pieceStructureBlockEntity.setAuthor(placer);
    }

    @Override
    public Piece getPiece() {
        return piece;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, EMPTY);
    }

    public static Direction getFacing(BlockState state) {
        return state.get(FACING);
    }

    public BlockBox getBuildingBox(BlockState state, BlockPos pos) {
        Direction facing = getFacing(state);
        BlockPos c1 = pos.offset(facing);
        BlockPos c2 = c1
                .offset(facing, PIECE_BASE_SIZE - 1)
                .offset(facing.rotateYCounterclockwise(), PIECE_BASE_SIZE - 1)
                .add(0, piece.getHeight() - 1, 0);
        return BlockBox.create(c1, c2);
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
        Direction playerFacing = ctx.getHorizontalPlayerFacing();
        Direction direction = ctx.shouldCancelInteraction() ? playerFacing.getOpposite() : playerFacing;
        return getDefaultState().with(FACING, direction);
    }

    /**
     * Drop contents
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (state.isOf(newState.getBlock()))
            return;

        if (state.get(EMPTY))
            return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PieceStructureBlockEntity structureBlockEntity) {
            if (world instanceof ServerWorld) {
                ItemScatterer.spawn(world, pos, new SimpleInventory(structureBlockEntity.getContent()));
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return EMPTY_COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(EMPTY))
            return EMPTY_COLLISION_SHAPE;
        return filledCollisionShape;
    }

    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }
}
