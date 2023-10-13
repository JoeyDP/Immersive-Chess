package be.immersivechess.block.entity;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class PieceStandBlockEntity extends DyedStructureRenderedBlockEntity {
    public final Piece piece;

    public PieceStandBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.PIECE_STAND_BLOCK_ENTITY_TYPE, pos, state);

        PieceContainer pieceGetter = (PieceContainer) state.getBlock();
        this.piece = pieceGetter.getPiece();
    }

}
