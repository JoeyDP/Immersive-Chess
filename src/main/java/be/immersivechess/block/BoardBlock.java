package be.immersivechess.block;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceItem;
import be.immersivechess.logic.MultiblockBoard;
import be.immersivechess.screen.ChessGameScreenHandler;
import be.immersivechess.world.ChessGameState;
import be.immersivechess.world.PieceRenderOption;
import ch.astorm.jchess.core.Color;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Blocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BoardBlock extends BlockWithEntity {

    private enum ColorState implements StringIdentifiable{
        WHITE,
        BLACK;

        @Override
        public String asString() {
            return toString().toLowerCase();
        }

        public static ColorState from(Color color){
            return color == Color.WHITE ? ColorState.WHITE : ColorState.BLACK;
        }
    }

    public static final EnumProperty<ColorState> COLOR = EnumProperty.of("color", ColorState.class);

    // do not use simplify or union (which calls simplify) because we want intermediate collision surfaces.
    private static final VoxelShape COLLISION_SHAPE = VoxelShapes.combine(
            VoxelShapes.combine(
                    Block.createCuboidShape(0, 0, 0, 16, 16, 16),
                    Block.createCuboidShape(1, 16, 1, 15, 24, 15),
                    BooleanBiFunction.OR),
            Block.createCuboidShape(2, 24, 2, 14, 32, 14),
            BooleanBiFunction.OR);

    public BoardBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(COLOR, ColorState.BLACK));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(player.getMainHandStack().isOf(Items.CHESS_CASE) || player.getMainHandStack().getItem() instanceof PieceItem)
            return ActionResult.PASS;

        if (!world.isClient){
            if (!(world.getBlockEntity(pos) instanceof BoardBlockEntity boardBlockEntity))
                return ActionResult.FAIL;

            // before opening screen, also perform some integrity checks
            ChessGameState gameState = boardBlockEntity.getGameState();
            if (gameState == null){
                ImmersiveChess.LOGGER.error("Failed to load game state associated with board -> reverting board blocks");
                MultiblockBoard board = MultiblockBoard.getValidBoard(world, player, pos);
                if (board != null)
                    board.endBoardBlocks(world);
                else
                    BoardBlock.placeBack(world, pos);

                return ActionResult.FAIL;
            }

            gameState.performIntegrityCheck();

            NamedScreenHandlerFactory screenHandlerFactory = createScreenHandlerFactory(state, world, pos);
            if (screenHandlerFactory == null)
                return ActionResult.FAIL;

            player.openHandledScreen(screenHandlerFactory);
            return ActionResult.success(true);
        }

        return ActionResult.success(true);
    }

    @Nullable
    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BoardBlockEntity boardBlockEntity))
            return null;

        ChessGameState gameState = boardBlockEntity.getGameState();
        if (gameState == null)
            return null;

        return new ExtendedScreenHandlerFactory() {
            @Override
            public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new ChessGameScreenHandler(i, gameState);
            }

            @Override
            public Text getDisplayName() {
                return Text.of("Chess Game");
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
                packetByteBuf.writeString(Optional.ofNullable(gameState.getPlayerName(Color.WHITE)).orElse(""));
                packetByteBuf.writeString(Optional.ofNullable(gameState.getPlayerName(Color.BLACK)).orElse(""));
                packetByteBuf.writeInt(gameState.getCurrentMoveIndex());
                packetByteBuf.writeEnumConstant(gameState.getColorOnMove());
                packetByteBuf.writeEnumConstant(gameState.getStatus());
                packetByteBuf.writeString(gameState.getDrawOfferedTo());
                packetByteBuf.writeIntArray(gameState.getValidRenderOptions(Color.WHITE).stream().mapToInt(Enum::ordinal).toArray());
                packetByteBuf.writeIntArray(gameState.getValidRenderOptions(Color.BLACK).stream().mapToInt(Enum::ordinal).toArray());
                packetByteBuf.writeEnumConstant(gameState.getRenderOption(Color.WHITE));
                packetByteBuf.writeEnumConstant(gameState.getRenderOption(Color.BLACK));
            }
        };
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    private void clearAbove(World world, BlockPos pos) {
        clearAbove(world, pos, false);
    }

    private void clearAbove(World world, BlockPos pos, boolean removeAll) {
        pos = pos.up();

        if (!removeAll) {
            // only chess pieces are allowed on the board unless removeAll is true
            BlockState state = world.getBlockState(pos);
            if(state.getBlock() instanceof PieceBlock || state.getBlock().equals((Blocks.AIR)))
                return;
        }

        breakBlockAboveBoard(world, pos);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return super.getAmbientOcclusionLightLevel(state, world, pos);
    }

    /**
     * Can be called instead of world.breakBlock when the block is directly above a board.
     * Overrides item drop logic to drop the loot one block higher to accommodate for the taller collision box.
     */
    public static void breakBlockAboveBoard(World world, BlockPos pos){
        // drop loot
        if (!world.isClient) {

            // based on `Block.dropStacks` to drop loot one block higher
            BlockState state = world.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            List<ItemStack> loot = Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, null, ItemStack.EMPTY);
            final BlockPos dropLocation = pos.up();
            loot.forEach(stack -> Block.dropStack(world, dropLocation, stack));
            state.onStacksDropped((ServerWorld) world, pos, ItemStack.EMPTY, true);
        }

        world.breakBlock(pos, false);
    }

    /**
     * Places a boardBlock to take the place of an existing block in the world. Stores info of original block
     */
    public void replace(World world, BlockPos pos, Color color) {
        world.setBlockState(pos, stateManager.getDefaultState().with(COLOR, ColorState.from(color)), Block.NOTIFY_ALL);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.isOf(state.getBlock()))
            return;

        moveEntitiesAbove(state, world, pos, oldState);

        // When we actually replace air, use default model instead of invisible air model.
        // Should only happen when manually placed
        if (oldState == Blocks.AIR.getDefaultState())
            oldState = state;

        // Clear invalid blocks directly above the board
        clearAbove(world, pos, true);

        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BoardBlockEntity boardBlockEntity) {
            boardBlockEntity.setOriginalBlockState(oldState);
        }
    }

    /**
     * Based on Block.pushEntitiesUpBeforeBlockChange, but using move instead of teleport
     */
    private void moveEntitiesAbove(BlockState state, World world, BlockPos pos, BlockState oldState){
        VoxelShape voxelShape = VoxelShapes.combine(oldState.getCollisionShape(world, pos), state.getCollisionShape(world, pos), BooleanBiFunction.ONLY_SECOND).offset(pos.getX(), pos.getY(), pos.getZ());
        if (voxelShape.isEmpty())
            return;

        List<Entity> list = world.getOtherEntities(null, voxelShape.getBoundingBox());
        for (Entity entity : list) {
            double d = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entity.getBoundingBox().offset(0.0, 1.0, 0.0), List.of(voxelShape), -1.0);
            if (entity instanceof LivingEntity){
                // LivingEntities tend to be larger and can be moved.
                entity.move(MovementType.SHULKER, new Vec3d(0.0, 1.0 + d, 0.0));
            }else{
                // Things like ItemEntities need teleportation (otherwise get stuck colliding with parts of shape)
                entity.requestTeleportOffset(0.0, 1.0 + d, 0.0);
            }
        }
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        // Move the player to the increased bounding box of the board.
        // (doesn't trigger when the entity is not directly above the block, which works out great for us)
        if (entity instanceof LivingEntity){
            // LivingEntities tend to be larger and can be moved.
            entity.move(MovementType.SHULKER, new Vec3d(Direction.UP.getUnitVector()));
        }else{
            // Things like ItemEntities need teleportation (otherwise get stuck colliding with parts of shape)
            entity.requestTeleportOffset(0.0, 1, 0.0);
        }
    }

    /**
     * Restores the original state of a position in the world where a BoardBlock exists.
     */
    public static void placeBack(WorldAccess world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BoardBlockEntity boardBlockEntity) {
            BlockState originalState = boardBlockEntity.getOriginalBlockState();
            world.setBlockState(pos, originalState, Block.NOTIFY_LISTENERS);
        } else {
            ImmersiveChess.LOGGER.error("No block Entity available to place correct blockState back from board.");
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (!newState.isOf(be.immersivechess.block.Blocks.BOARD_BLOCK)){
            clearAbove(world, pos, true);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BoardBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof BoardBlockEntity boardBlockEntity){
            BlockState appearanceBlockState = boardBlockEntity.getOriginalBlockState();
            return appearanceBlockState.getOpacity(world, pos);
        }

        return super.getOpacity(state, world, pos);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockRenderView renderView, BlockPos pos, Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        // This function only reports what the block looks like, it doesn't actually change it. See BoardModel
        RenderAttachedBlockView attachmentView = (RenderAttachedBlockView) renderView;
        Object data = attachmentView.getBlockEntityRenderAttachment(pos);

        // Check if data is not null and of the correct type, and use that to determine the appearance
        if (data instanceof BlockState appearanceBlockState) {
            return appearanceBlockState;
        }

        return state;
    }

}
