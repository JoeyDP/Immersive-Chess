package be.immersivechess.block.entity;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.logic.Piece;
import be.immersivechess.util.BlockStateUtil;
import be.immersivechess.world.ChessGameState;
import ch.astorm.jchess.core.Coordinate;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BoardBlockEntity extends BlockEntity implements RenderAttachmentBlockEntity {

    private static final String GAME_ID_KEY = "GameId";
    private static final String BLOCKSTATE_KEY = "OriginalBlockState";
    private static final String SQUARE_KEY = "Square";
    private static final String PIECE_KEY = "Piece";
    private static final String IN_CHECK_KEY = "InCheck";
    private static final String WHITE_DIRECTION_KEY = "Direction";

    private ChessGameState gameState;

    // stored in NBT and synced to client when updated (for rendering)
    private String gameSaveId;          // date and id
    private BlockState originalBlockState = Blocks.AIR.getDefaultState();
    private Coordinate square;
    private Direction whitePlayDirection;
    private boolean inCheck;            // set by ChessGameState
    private Piece piece;                // only set when piece mined to indicate as placeholder

    public BoardBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.BOARD_BLOCK_ENTITY_TYPE, pos, state);
    }

    public void setOriginalBlockState(@Nullable BlockState blockState) {
        if (blockState == originalBlockState)
            return;

        if (blockState == null)
            blockState = Blocks.AIR.getDefaultState();

        originalBlockState = blockState;
        markDirty();
        updateBlockModel();
    }

    public void setGameState(@NotNull ChessGameState gameState) {
        this.gameState = gameState;
        if (!gameState.getGameSaveId().equals(gameSaveId)) {
            this.gameSaveId = gameState.getGameSaveId();
            setWhitePlayDirection(gameState.getWhitePlayDirection());
            markDirty();
        }
    }


    public void setSquare(Coordinate square) {
        this.square = square;
        markDirty();
        syncToClient();
    }

    public Coordinate getSquare() {
        return square;
    }

    public void setPiece(Piece piece){
        if (this.piece != null && this.piece.equals(piece))
            return;

        this.piece = piece;
        markDirty();
        syncToClient();
    }

    public Piece getPiece() {
        return piece;
    }

    public void setInCheck(boolean isUnderAttack){
        if (this.inCheck == isUnderAttack)
            return;

        this.inCheck = isUnderAttack;
        markDirty();
        syncToClient();
    }

    public boolean isInCheck() {
        return inCheck;
    }

    public Direction getWhitePlayDirection(){
        return whitePlayDirection;
    }

    public String getGameSaveId() {
        return gameSaveId;
    }

    public ChessGameState getGameState() {
        return gameState;
    }

    public BlockState getOriginalBlockState() {
        return originalBlockState;
    }

    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if (world instanceof ServerWorld serverWorld && gameSaveId != null){
            gameState = ChessGameState.get(serverWorld, gameSaveId);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(BLOCKSTATE_KEY)) {
            originalBlockState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound(BLOCKSTATE_KEY));
            updateBlockModel();
        }

        if (nbt.contains(GAME_ID_KEY))
            // Actual game is loaded when world gets set (depends on persistent state)
            gameSaveId = nbt.getString(GAME_ID_KEY);

        if (nbt.contains(SQUARE_KEY))
            square = new Coordinate(nbt.getString(SQUARE_KEY));

        if (nbt.contains(WHITE_DIRECTION_KEY))
            whitePlayDirection = Direction.fromHorizontal(nbt.getInt(WHITE_DIRECTION_KEY));

        if (nbt.contains(IN_CHECK_KEY))
            inCheck = nbt.getBoolean(IN_CHECK_KEY);

        try{
            piece = nbt.contains(PIECE_KEY) ? Piece.fromName(nbt.getString(PIECE_KEY)) : null;
        } catch (IllegalArgumentException e){
            ImmersiveChess.LOGGER.error("Failed to load piece from nbt string", e);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (originalBlockState != null)
            nbt.put(BLOCKSTATE_KEY, NbtHelper.fromBlockState(originalBlockState));
        if (gameSaveId != null)
            nbt.putString(GAME_ID_KEY, gameSaveId);
        if (square != null)
            nbt.putString(SQUARE_KEY, square.toString());
        if (piece != null)
            nbt.putString(PIECE_KEY, piece.toString());
        if (whitePlayDirection != null)
            nbt.putInt(WHITE_DIRECTION_KEY, whitePlayDirection.getHorizontal());
        nbt.putBoolean(IN_CHECK_KEY, inCheck);
    }

    @Override
    public @Nullable Object getRenderAttachmentData() {
        return originalBlockState;
    }

    private void setWhitePlayDirection(Direction direction){
        if (!Objects.equals(whitePlayDirection, direction)){
            this.whitePlayDirection = direction;
            syncToClient();
        }
        // no mark dirty needed because this is set when gamestate loaded
    }

    private void updateBlockModel() {
        if (world == null)
            return;

        world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
    }

    private void syncToClient(){
        // invoke sync of blockentity state to client
        if (world instanceof ServerWorld serverWorld)
            serverWorld.getChunkManager().markForUpdate(pos);
    }
}
