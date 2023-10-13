package be.immersivechess.screen;


import be.immersivechess.item.PieceStandItem;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

class FilteredPieceSlot extends Slot {
    private final Piece expectedPiece;
    public FilteredPieceSlot(Inventory inventory, int index, int x, int y, Piece expectedPiece) {
        super(inventory, index, x, y);
        this.expectedPiece = expectedPiece;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (!(stack.getItem() instanceof PieceStandItem))
            return false;
        Piece piece = PieceContainer.getPiece(stack);
        if(piece == null){
            return true;        // allow empty cards in
        }
        if (piece != expectedPiece)
            return false;
        return true;
    }
}
