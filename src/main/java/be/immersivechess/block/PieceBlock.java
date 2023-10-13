package be.immersivechess.block;

import be.immersivechess.block.entity.BlockEntityTypes;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.block.entity.PieceBlockEntity;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.PieceItem;
import be.immersivechess.logic.MultiblockBoard;
import be.immersivechess.logic.Piece;
import be.immersivechess.world.ChessGameState;
import ch.astorm.jchess.core.Color;
import ch.astorm.jchess.core.Coordinate;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PieceBlock extends BlockWithEntity implements PieceContainer {

    private final Piece piece;

    private final VoxelShape outlineShape;
    private final VoxelShape collisionShape;

    // Blockstate properties
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public PieceBlock(Settings settings, Piece piece) {
        super(settings);
        this.piece = piece;
        outlineShape = Block.createCuboidShape(0, 0, 0, 16, 2 * piece.getHeight(), 16);
        // allow 3/16 of space so board collision can help the player move up over pieces
        collisionShape = Block.createCuboidShape(3, 0, 3, 13, 2 * piece.getHeight(), 13);
        setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    public Piece getPiece() {
        return piece;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public PieceBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PieceBlockEntity(pos, state);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state);
        world.getBlockEntity(pos, BlockEntityTypes.PIECE_BLOCK_ENTITY_TYPE).ifPresent(blockEntity -> {
            blockEntity.setStackNbt(itemStack);
            // setStackNbt also sets the id of the entity which is not needed and causes duplication. Remove it.
            if (itemStack.hasNbt() && itemStack.getNbt().contains(BlockItem.BLOCK_ENTITY_TAG_KEY))
                itemStack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY).remove("id");
        });
        return itemStack;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        ChessGameState gameState = getGameState(world, pos);
        if (gameState == null) return;
        Coordinate source = PieceContainer.getSourceSquare(itemStack);
        if (source == null) return;
        gameState.doMove(source, getSquare(world, pos), piece);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (world.isClient()) return false;
        if (!(world.getBlockEntity(pos.down()) instanceof BoardBlockEntity boardBlockEntity)) return false;
        ChessGameState gameState = boardBlockEntity.getGameState();
        if (gameState == null) return false;

        return true;
    }

    /**
     * Checks whether it's the player's turn and they haven't mined a piece already
     */
    private boolean canMinePiece(World world, BlockPos pos, PlayerEntity player) {
        ChessGameState gameState = getGameState(world, pos);
        if (gameState == null) return true;
        Coordinate square = getSquare(world, pos);
        if (square == null) return true;
        return gameState.canMinePiece(square, player);
    }

    private boolean canCapturePiece(World world, BlockPos pos, PlayerEntity player) {
        ChessGameState gameState = getGameState(world, pos);
        if (gameState == null) return false;
        ItemStack movingPiece = player.getMainHandStack();
        if (!(movingPiece.getItem() instanceof PieceItem pieceItem)) return false;

        return PieceContainer.getDestinationSquares(movingPiece).contains(getSquare(world, pos));
    }

    /**
     * Get square of piece based on board block below it
     */
    @Nullable
    private Coordinate getSquare(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos.down());
        if (blockEntity instanceof BoardBlockEntity boardBlockEntity)
            return boardBlockEntity.getSquare();
        return null;
    }

    /**
     * Get ChessGameState based on board
     */
    @Nullable
    private ChessGameState getGameState(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos.down());
        if (blockEntity instanceof BoardBlockEntity boardBlockEntity)
            return boardBlockEntity.getGameState();
        return null;
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        // Allows player to replace opponent pieces.
        // Does not check for valid moves, only whether replacement is allowed to happen.
        if (!(context.getStack().getItem() instanceof PieceItem pieceItem))
            return false;

        Color myColor = ((PieceBlock) state.getBlock()).getPiece().getColor();
        return pieceItem.getPiece().getColor() != myColor;
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (world.isClient)
            return;

        ChessGameState gameState = getGameState(world, pos);
        // If not part of game, do regular mining
        if (gameState == null) {
            world.breakBlock(pos, false, player);
            return;
        }

        // If piece in hand, call replace instead of trying to mine opponent piece.
        if (canCapturePiece(world, pos, player)) {
            PieceItem pieceItem = (PieceItem) player.getMainHandStack().getItem();
            if (replaceOpponentPiece(world, pos, player, pieceItem, gameState))
                return;
        }

        // Try to mine piece if valid
        if (canMinePiece(world, pos, player)) {
            minePiece(world, pos, player, gameState);
        }
    }

    private void minePiece(World world, BlockPos pos, PlayerEntity player, ChessGameState gameState) {
        if (!world.isClient) {
            // loot
            BlockState state = world.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            List<ItemStack> loot = Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, null, ItemStack.EMPTY);
            state.onStacksDropped((ServerWorld) world, pos, player.getMainHandStack(), true);
            loot.forEach(stack -> player.getInventory().offerOrDrop(stack));
            // update gamestate
            gameState.setMinedSquare(pos);
        }

        world.breakBlock(pos, false);
    }

    private boolean replaceOpponentPiece(World world, BlockPos pos, PlayerEntity player, PieceItem pieceItem, ChessGameState gameState) {
        ItemStack stack = player.getMainHandStack();
        BlockState blockState = pieceItem.getBlock().getDefaultState();
        ItemPlacementContext context = new ItemPlacementContext(player, Hand.MAIN_HAND, stack, new BlockHitResult(Vec3d.ofBottomCenter(pos), Direction.UP, pos, false));
        if (canPlaceAt(blockState, world, pos) && canReplace(world.getBlockState(pos), context)) {
            if (pieceItem.place(context).isAccepted())
                return true;
        }
        return false;
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        // We have custom block breaking code. This is used to prevent MC from breaking blocks
        return 0;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        // if not on an ongoing game, don't drop anything
        BlockPos pos = BlockPos.ofFloored(builder.get(LootContextParameters.ORIGIN));
        ChessGameState gameState = getGameState(builder.getWorld(), pos);
        if (gameState == null || gameState.getStatus().isFinished()) return Collections.emptyList();

        // if possible destination of pawn is backline -> drop promotion pieces instead
        List<ItemStack> normalLoot = super.getDroppedStacks(state, builder);
        for (ItemStack stack : normalLoot) {
            if (isPromotablePawn(stack)) {
                return getPromotionPieces(gameState, stack);
            }
        }

        return normalLoot;
    }

    private boolean isPromotablePawn(ItemStack stack) {
        if (!(piece == Piece.WHITE_PAWN || piece == Piece.BLACK_PAWN)) return false;
        // Redundant checks if Minecraft does not mess up
//        if (!(stack.getItem() instanceof PieceItem pieceItem)) return false;
//        if (pieceItem.getPiece() != piece) return false;

        List<Coordinate> destinations = PieceContainer.getDestinationSquares(stack);
        for (Coordinate destination : destinations) {
            if (destination.getRow() == 0 || destination.getRow() == MultiblockBoard.BOARD_SIZE - 1)
                return true;
        }
        return false;
    }

    /**
     * Pieces that can be placed when promotion is possible. This includes:
     * - the original pawn with the origin square and no other destinations (for placing back)
     * - all other pieces of the same color with the pawn destinations
     */
    private List<ItemStack> getPromotionPieces(ChessGameState gameState, ItemStack pawn) {
        gameState.getStructure(piece);

        Coordinate source = PieceContainer.getSourceSquare(pawn);
        List<Coordinate> destinations = PieceContainer.getDestinationSquares(pawn);
        destinations.remove(source);
        String gameId = PieceContainer.getGameSaveId(pawn);
        int moveIndex = PieceContainer.getMoveIndex(pawn);

        List<ItemStack> loot = new ArrayList<>();

        PieceContainer.writeDestinations(pawn, List.of(source));
        loot.add(pawn);

        for (Piece promotionPiece : piece.getPromotions()) {
            ItemStack stack = new ItemStack(Items.PIECE_ITEMS.get(promotionPiece), 1);
            PieceContainer.writeStructureNbt(stack, gameState.getStructure(promotionPiece));
            PieceContainer.writeGameSaveId(stack, gameId);
            PieceContainer.writeSourceSquare(stack, source);
            PieceContainer.writeDestinations(stack, destinations);
            PieceContainer.writeMoveIndex(stack, moveIndex);

            loot.add(stack);
        }

        return loot;
    }

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
        // This is the default behaviour. When the itemstack contains blockstate info in nbt, it is applied instead.
        Direction playerFacing = ctx.getHorizontalPlayerFacing();
        Direction direction = ctx.shouldCancelInteraction() ? playerFacing.getOpposite() : playerFacing;
        return getDefaultState().with(FACING, direction);
    }

    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return outlineShape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return collisionShape;
    }

//    @Override
//    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
//        return shape;
//    }

}
