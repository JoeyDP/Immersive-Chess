package be.immersivechess.client.render.model;


import be.immersivechess.ImmersiveChess;
import be.immersivechess.logic.Piece;
import ch.astorm.jchess.core.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ModelProvider implements ModelResourceProvider {

    private static final Identifier BOARD_BLOCK_BLACK = new Identifier(ImmersiveChess.MOD_ID, "block/board_black");
    private static final Identifier BOARD_BLOCK_WHITE = new Identifier(ImmersiveChess.MOD_ID, "block/board_white");

    private static final Identifier BOARD_BLOCK_ITEM = new Identifier(ImmersiveChess.MOD_ID, "item/board");

    @Override
    public @Nullable UnbakedModel loadModelResource(Identifier identifier, ModelProviderContext modelProviderContext) {
        if (!identifier.getNamespace().equals(ImmersiveChess.MOD_ID))
            return null;

        // Board Block models
        if (identifier.equals(BOARD_BLOCK_BLACK))
            return new BoardBlockModel(Color.BLACK);
        if (identifier.equals(BOARD_BLOCK_WHITE))
            return new BoardBlockModel(Color.WHITE);

        // Board Item
        if (identifier.equals(BOARD_BLOCK_ITEM))
            return new BoardBlockModel(Color.BLACK);

        // Piece Block models
        if (identifier.getPath().startsWith("block/piece/")) {
            Piece piece = Piece.fromName(identifier.getPath().replaceFirst("block/piece/", ""));
            return new PieceModel(piece);
        }

        // Piece Item models
        if (identifier.getPath().startsWith("item/piece/")) {
            Piece piece = Piece.fromName(identifier.getPath().replaceFirst("item/piece/", ""));
            return new PieceModel(piece);
        }

        // Piece Stand Block models
        if (identifier.getPath().startsWith("block/stand/")) {
            Piece piece = Piece.fromName(identifier.getPath().replaceFirst("block/stand/", ""));
            return new PieceStandModel(piece);
        }

        // Piece Stand Item models
        if (identifier.getPath().startsWith("item/stand/")) {
            Piece piece = Piece.fromName(identifier.getPath().replaceFirst("item/stand/", ""));
            return new PieceStandModel(piece);
        }

        // Piece Structure Block models
        if (identifier.getPath().startsWith("block/structure/")) {
            Piece piece = Piece.fromName(identifier.getPath().replaceFirst("block/structure/", ""));
            return new PieceStructureBlockModel(piece);
        }

        return null;
    }
}

