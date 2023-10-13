package be.immersivechess.screen;

import be.immersivechess.item.ChessCase;
import be.immersivechess.logic.Piece;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ChessCaseScreenHandler extends ScreenHandler {

    private final Inventory inventory;

    private final ItemStack caseItemStack;

    private final int INVENTORY_SIZE = 2 * 6;

    public ChessCaseScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2 * 6), ItemStack.EMPTY);
    }

    public ChessCaseScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ItemStack caseItemStack){
        super(ScreenHandlerTypes.CHESS_CASE_SCREEN_HANDLER_TYPE, syncId);
        this.inventory = inventory;
        this.caseItemStack = caseItemStack;
        inventory.onOpen(playerInventory.player);

        // chess case inventory
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 6; ++col) {
                Piece piece = Piece.values()[col + row * 6];
                this.addSlot(new FilteredPieceSlot(inventory, col + row * 6, 27 + col * 21, 23 + row * 23, piece));
            }
        }

        // player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();
            if(slot < INVENTORY_SIZE){
                if(!this.insertItem(itemStack2, INVENTORY_SIZE, this.slots.size(), true)){
                    return ItemStack.EMPTY;
                }
            }else{
                if(!this.insertItem(itemStack2, 0, INVENTORY_SIZE, false)){
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }
        return itemStack;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        // NOTE: this function doesn't seem to prevent items from being put into the slot at all..
        // case can only contain specific items
        if(slot.inventory == this.inventory){
            return ChessCase.canContainItem(stack.getItem()) && super.canInsertIntoSlot(stack, slot);
        }
        return super.canInsertIntoSlot(stack, slot);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (caseItemStack.isEmpty())
            return;

//        ImmersiveChess.LOGGER.info("Screen closed");
        ChessCase.setOpen(caseItemStack, false);
    }
}
