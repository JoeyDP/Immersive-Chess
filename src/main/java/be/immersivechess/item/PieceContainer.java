package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.entity.PieceBlockEntity;
import be.immersivechess.logic.Piece;
import ch.astorm.jchess.core.Coordinate;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface PieceContainer {

    // under BlockItem.BLOCK_ENTITY_TAG_KEY
    String NBT_STRUCTURE_KEY = "Structure";
    String NBT_COLOR_KEY = "Color";

    // in root nbt
    String NBT_GAME_ID_KEY = "GameId";
    String NBT_MOVE_INDEX_KEY = "MoveIndex";
    String NBT_SOURCE_SQUARE_KEY = "SourceSquare";
    String NBT_DESTINATION_SQUARES_KEY = "DestinationSquares";

    Piece getPiece();

    static Piece getPiece(ItemStack stack) {
        if (stack.getItem() instanceof PieceContainer pieceContainer)
            return pieceContainer.getPiece();
        return null;
    }

    static void writeStructureNbt(ItemStack stack, NbtCompound structureNbt) {
        NbtCompound nbt = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
        nbt.put(NBT_STRUCTURE_KEY, structureNbt);
    }

    @NotNull
    static NbtCompound getStructureNbt(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
        if (nbt != null && nbt.contains(PieceContainer.NBT_STRUCTURE_KEY))
            return nbt.getCompound(PieceContainer.NBT_STRUCTURE_KEY);

        // empty compound indicates use of default structure
        return new NbtCompound();
    }

    static void writeColor(ItemStack stack, int color) {
        NbtCompound nbt = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
        nbt.putInt(NBT_COLOR_KEY, color);
    }

    static int getColor(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
        if (nbt != null && nbt.contains(PieceContainer.NBT_COLOR_KEY))
            return nbt.getInt(PieceContainer.NBT_COLOR_KEY);

        return StandItem.DEFAULT_COLOR_INT;
    }

    static void removeColor(ItemStack stack){
        NbtCompound nbt = stack.getSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
        if (nbt == null) return;

        if (nbt.contains(PieceContainer.NBT_COLOR_KEY))
            nbt.remove(PieceContainer.NBT_COLOR_KEY);

        if (nbt.isEmpty())
            stack.removeSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
    }

    static NbtCompound getGameInfo(ItemStack stack){
        if (!stack.hasNbt() || !stack.getNbt().contains(PieceBlockEntity.NBT_GAME_INFO)) return null;
        return stack.getNbt().getCompound(PieceBlockEntity.NBT_GAME_INFO);
    }

    static NbtCompound getOrCreateGameInfo(ItemStack stack){
        return stack.getOrCreateSubNbt(PieceBlockEntity.NBT_GAME_INFO);
    }

    /**
     * Extract origin from where this piece was mined. Not every PieceContainer will have this.
     */
    @Nullable
    static Coordinate getSourceSquare(ItemStack stack) {
        NbtCompound nbt = getGameInfo(stack);
        if (nbt == null || !nbt.contains(NBT_SOURCE_SQUARE_KEY)) return null;

        String squareStr = nbt.getString(NBT_SOURCE_SQUARE_KEY);
        try{
            return new Coordinate(squareStr);
        } catch (IllegalArgumentException e){
            ImmersiveChess.LOGGER.error("Failed to convert piece nbt to square", e);
            return null;
        }
    }

    @Nullable
    static String getGameSaveId(ItemStack stack) {
        NbtCompound nbt = getGameInfo(stack);
        if (nbt == null || !nbt.contains(NBT_GAME_ID_KEY)) return null;

        return nbt.getString(NBT_GAME_ID_KEY);
    }

    /**
     * Gets the index this move would be once the piece is placed. Returning 0 means missing value.
     */
    static int getMoveIndex(ItemStack stack) {
        NbtCompound nbt = getGameInfo(stack);
        if (nbt == null || !nbt.contains(NBT_MOVE_INDEX_KEY)) return 0;

        return nbt.getInt(NBT_MOVE_INDEX_KEY);
    }

    static List<Coordinate> getDestinationSquares(ItemStack stack){
        NbtCompound nbt = getGameInfo(stack);
        if (nbt == null || !nbt.contains(NBT_DESTINATION_SQUARES_KEY)) return Collections.emptyList();

        NbtList squareStrs = nbt.getList(NBT_DESTINATION_SQUARES_KEY, NbtElement.STRING_TYPE);
        List<Coordinate> destinations = new ArrayList<>();
        for (int i =0; i < squareStrs.size(); i++){
            String squareStr = squareStrs.getString(i);
            try{
                destinations.add(new Coordinate(squareStr));
            } catch (IllegalArgumentException e){
                ImmersiveChess.LOGGER.error("Failed to convert piece nbt to square", e);
            }
        }
        return destinations;
    }

    static void writeGameSaveId(ItemStack stack, String gameId){
        getOrCreateGameInfo(stack).putString(NBT_GAME_ID_KEY, gameId);
    }

    static void writeMoveIndex(ItemStack stack, int moveIndex){
        getOrCreateGameInfo(stack).putInt(NBT_MOVE_INDEX_KEY, moveIndex);
    }

    /**
     * Write origin from where this piece was mined from.
     */
    static void writeSourceSquare(ItemStack stack, Coordinate square) {
        getOrCreateGameInfo(stack).putString(NBT_SOURCE_SQUARE_KEY, square.toString());
    }

    /**
     * Write valid move destinations to stack nbt.
     */
    static void writeDestinations(ItemStack stack, List<Coordinate> destinations) {
        NbtList nbtDestinations = new NbtList();
        destinations.stream()
                .map(Coordinate::toString)
                .forEach(s -> nbtDestinations.add(NbtString.of(s)));
        getOrCreateGameInfo(stack).put(PieceContainer.NBT_DESTINATION_SQUARES_KEY, nbtDestinations);
    }


}
