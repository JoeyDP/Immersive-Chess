package be.immersivechess.block.entity;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import be.immersivechess.world.ChessGameState;
import ch.astorm.jchess.core.Coordinate;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PieceBlockEntity extends StructureRenderedBlockEntity {

    public static final String NBT_GAME_INFO = "GameInfo";

    public final Piece piece;

    public PieceBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.PIECE_BLOCK_ENTITY_TYPE, pos, state);

        PieceContainer pieceGetter = (PieceContainer) state.getBlock();
        this.piece = pieceGetter.getPiece();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        // nbt data can be passed along to item when mined if configured in loot table
        if (!hasWorld() || world.isClient)
            return;

        // during world save, don't store gameplay data to piece (only needed for item form)
        if (world.getServer().isSaving())
            return;

        // all information is fetched from the board
        BlockEntity blockEntity = world.getBlockEntity(getPos().down());
        if (blockEntity instanceof BoardBlockEntity boardBlockEntity) {
            // Also write game data to nbt so it can be read by the mined item.
            // Does not need to be read as it is determined from game state.
            NbtCompound gameInfo = new NbtCompound();

            // game id
            if (boardBlockEntity.getGameSaveId() != null)
                gameInfo.putString(PieceContainer.NBT_GAME_ID_KEY, boardBlockEntity.getGameSaveId());

            // origin
            Coordinate origin = boardBlockEntity.getSquare();
            if (origin != null)
                gameInfo.putString(PieceContainer.NBT_SOURCE_SQUARE_KEY, origin.toString());

            ChessGameState gameState = boardBlockEntity.getGameState();
            if (gameState != null) {
                // possible destination squares
                NbtList destinations = new NbtList();
                gameState.getLegalDestinations(origin).stream()
                        .map(Coordinate::toString)
                        .forEach(s -> destinations.add(NbtString.of(s)));
                gameInfo.put(PieceContainer.NBT_DESTINATION_SQUARES_KEY, destinations);

                // moveIndex
                gameInfo.putInt(PieceContainer.NBT_MOVE_INDEX_KEY, gameState.getCurrentMoveIndex());
            }

            nbt.put(NBT_GAME_INFO, gameInfo);
        }
    }
}
