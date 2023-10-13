package be.immersivechess.item;

import be.immersivechess.block.PieceStandBlock;
import be.immersivechess.logic.Piece;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeableItem;

/**
 * Serves as a template for the real pieces. Contains a structure in Nbt.
 */
public class PieceStandItem extends StandItem implements PieceContainer {

    private final Piece piece;

    public PieceStandItem(PieceStandBlock pieceStandBlock, Settings settings) {
        super(pieceStandBlock, settings);
        this.piece = pieceStandBlock.getPiece();
    }

    @Override
    public Piece getPiece() {
        return piece;
    }

}
