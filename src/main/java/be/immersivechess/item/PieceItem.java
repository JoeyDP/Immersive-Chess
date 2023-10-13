package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.PieceBlock;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.logic.Piece;
import be.immersivechess.world.ChessGameState;
import ch.astorm.jchess.core.Coordinate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PieceItem extends BlockItem implements PieceContainer {

    public static final TagKey<Item> PIECE_TAG = TagKey.of(RegistryKeys.ITEM, new Identifier(ImmersiveChess.MOD_ID, "piece"));

    private final Piece piece;

    public PieceItem(PieceBlock pieceBlock, Settings settings) {
        super(pieceBlock, settings);
        this.piece = pieceBlock.getPiece();
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (removeIfInvalid(stack, world, entity, slot)) return;
    }

    /**
     * Removes piece from inventory if not valid. Returns true if it was removed
     */
    private boolean removeIfInvalid(ItemStack stack, World world, Entity entity, int slot){
        if (!(world instanceof ServerWorld serverWorld)) return false;
        if (entity instanceof PlayerEntity player && player.isCreativeLevelTwoOp()) return false;

        // invalid piece -> discard
        String gameId = PieceContainer.getGameSaveId(stack);
        if (gameId == null) return removeFromInventory(entity, slot);
        ChessGameState gameState = ChessGameState.get(serverWorld, gameId);
        if (gameState == null) return removeFromInventory(entity, slot);
        int turn = PieceContainer.getMoveIndex(stack);
        if (turn == 0) return removeFromInventory(entity, slot);

        // game finished -> discard
        // Note: not using isPlayAllowed because for a draw this would be true
        if (gameState.getStatus().isFinished()) return removeFromInventory(entity, slot);

        // piece from different turn -> discard
        if (!gameState.hasPlayerMinedPiece() || turn != gameState.getCurrentMoveIndex()) return removeFromInventory(entity, slot);

        // piece from another player -> drop
        if (entity instanceof PlayerEntity playerEntity && !gameState.isPlayerOnMove(playerEntity)) return dropFromInventory(entity, slot);

        return false;
    }

    private boolean removeFromInventory(Entity entity, int slot){
        if (entity instanceof PlayerEntity playerEntity){
            playerEntity.getInventory().removeStack(slot);
            return true;
        }
        return false;
    }

    private boolean dropFromInventory(Entity entity, int slot){
        if (entity instanceof PlayerEntity playerEntity){
            ItemStack stack = playerEntity.getInventory().removeStack(slot);
            playerEntity.dropItem(stack, false);
            return true;
        }
        return false;
    }

    @Override
    public Piece getPiece() {
        return piece;
    }

    @Override
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        // TODO: might override player collision check here as well
        if (!super.canPlace(context, state))
            return false;

        if (context.getWorld().isClient) return false;
        if (context.getPlayer() != null && context.getPlayer().isCreativeLevelTwoOp()) return true;

        // if not on board don't allow placement
        if (!(context.getWorld().getBlockEntity(context.getBlockPos().down()) instanceof BoardBlockEntity boardBlockEntity)) return false;
        ChessGameState gameState = boardBlockEntity.getGameState();
        if (gameState == null) return false;

        Coordinate destination = boardBlockEntity.getSquare();
        return PieceContainer.getDestinationSquares(context.getStack()).contains(destination);
    }
}
