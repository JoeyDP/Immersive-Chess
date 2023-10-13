package be.immersivechess.client.render.block.entity;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.PieceItem;
import be.immersivechess.logic.Piece;
import be.immersivechess.recipe.StandDyeRecipe;
import ch.astorm.jchess.core.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BoardBlockEntityRenderer implements BlockEntityRenderer<BoardBlockEntity> {

    private final BlockEntityRendererFactory.Context context;

    private static final SpriteIdentifier SQUARE_SPRITE_ID = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ImmersiveChess.MOD_ID, "block/highlight_square"));
    private static final SpriteIdentifier ATTACK_SPRITE_ID = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ImmersiveChess.MOD_ID, "block/highlight_attack"));
    private static final SpriteIdentifier DOT_SPRITE_ID = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ImmersiveChess.MOD_ID, "block/highlight_dot"));

    private static final int WHITE_COLOR = ColorHelper.Argb.getArgb(255, 255, 255, 255);
    private static final int RED_COLOR = StandDyeRecipe.dyeColorToColor(DyeColor.RED);

    public BoardBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public int getRenderDistance() {
        return BlockEntityRenderer.super.getRenderDistance();
    }

    @Override
    public void render(BoardBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
//        if (entity.getSquare() != null)
//            renderSquare(entity, matrices, vertexConsumers, light);

        if (shouldHighlightDestinations(entity))
            renderPossiblePositionHighlight(entity, matrices, vertexConsumers, light);

        Piece piece = entity.getPiece();
        if (piece != null)
            renderOriginHighlight(entity, matrices, vertexConsumers, light, piece);

        if (entity.isInCheck())
            renderKingUnderAttackHighlight(entity, matrices, vertexConsumers, light);
    }

    /**
     * Render square coordinate above block. Mostly for debugging
     */
    public void renderSquare(BoardBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        String text = entity.getSquare().toString();
        matrices.push();
        float scale = 0.07f;
        int color = 0xFFFFFF;
        light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        matrices.translate(0.1, 1.001, 0.2);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.scale(scale, scale, scale);
        context.getTextRenderer().draw(text, 0, 0, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
        matrices.pop();
    }

    /**
     * Returns the held itemstack by the player if it belongs to this game. Null otherwise.
     */
    private ItemStack getPlayerHoldingIfPieceOfGame(BoardBlockEntity entity) {
        if (MinecraftClient.getInstance().player == null) return null;

        ItemStack playerHolding = MinecraftClient.getInstance().player.getMainHandStack();
        if (!(playerHolding.getItem() instanceof PieceItem)) return null;

        String gameId = PieceContainer.getGameSaveId(playerHolding);
        if (gameId == null || !gameId.equals(entity.getGameSaveId())) return null;

        return playerHolding;
    }

    private boolean shouldHighlightDestinations(BoardBlockEntity entity) {
        ItemStack playerHolding = getPlayerHoldingIfPieceOfGame(entity);
        if (playerHolding == null) return false;

        return PieceContainer.getDestinationSquares(playerHolding).contains(entity.getSquare());
    }

    public void renderPossiblePositionHighlight(BoardBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        VertexConsumer consumer = SQUARE_SPRITE_ID.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

        // determine color of piece
        ItemStack playerHolding = MinecraftClient.getInstance().player.getMainHandStack();
        if (!(playerHolding.getItem() instanceof PieceContainer pieceContainer))
            return;

        matrices.push();
        matrices.translate(0, 1, 0);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        // can have color depend on type, but black is not always visible.
        renderSquare(positionMatrix, normalMatrix, consumer, 0, 0.001f, 0, 0, 0, WHITE_COLOR);

        matrices.pop();
    }

    public void renderOriginHighlight(BoardBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Piece piece) {
        Identifier iconIdentifier = piece.getIdentifier().withPrefixedPath("item/icon/");
        SpriteIdentifier spriteId = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, iconIdentifier);
        VertexConsumer consumer = spriteId.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

        Direction facing = entity.getWhitePlayDirection();
        if (piece.getColor().equals(Color.BLACK))
            facing = facing.getOpposite();

        matrices.push();
        matrices.translate(0.5, 1, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180 + facing.asRotation()));
        matrices.translate(-0.5, 0, -0.5);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        renderSquare(positionMatrix, normalMatrix, consumer, 0, 0.001f, 0, 0, 0);
        matrices.pop();
    }

    public void renderKingUnderAttackHighlight(BoardBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        VertexConsumer consumer = ATTACK_SPRITE_ID.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

        matrices.push();
        matrices.translate(0, 1, 0);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        renderSquare(positionMatrix, normalMatrix, consumer, 0, 0.001f, 0, 0, 0, RED_COLOR);

        matrices.pop();
    }

    private static void renderSquare(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer, float x, float y, float z, float u, float v) {
        renderSquare(positionMatrix, normalMatrix, consumer, x, y, z, u, v, ColorHelper.Argb.getArgb(255, 255, 255, 255));
    }

    private static void renderSquare(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer, float x, float y, float z, float u, float v, int color) {
        float uv_offset = 1f;
        vertex(positionMatrix, normalMatrix, consumer, x, y, z, u, v, color);
        vertex(positionMatrix, normalMatrix, consumer, x, y, z + 1, u, v + uv_offset, color);
        vertex(positionMatrix, normalMatrix, consumer, x + 1, y, z + 1, u + uv_offset, v + uv_offset, color);
        vertex(positionMatrix, normalMatrix, consumer, x + 1, y, z, u + uv_offset, v, color);
    }

    /**
     * @param v the top-most coordinate of the texture region
     * @param u the left-most coordinate of the texture region
     */
    private static void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer, float x, float y, float z, float u, float v, int color) {
        // Note: chaining these operations together turns the VertexConsumer from a SpriteTexturedVertexConsumer into another one.
        consumer.vertex(positionMatrix, x, y, z);
        consumer.color(color);
        consumer.texture(u, v);
        consumer.overlay(OverlayTexture.DEFAULT_UV);
        consumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
        consumer.normal(normalMatrix, 0.0f, 1.0f, 0.0f);
        consumer.next();
    }

}
