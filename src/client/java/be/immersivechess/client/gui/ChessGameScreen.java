package be.immersivechess.client.gui;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.screen.ChessGameScreenHandler;
import be.immersivechess.world.PieceRenderOption;
import ch.astorm.jchess.core.Color;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.DataFixUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ChessGameScreen extends HandledScreen<ChessGameScreenHandler> {

    private static final Identifier TEXTURE = new Identifier(ImmersiveChess.MOD_ID, "textures/gui/chess_game.png");
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_PADDING = 8;

    private final ButtonWidget drawButton = ButtonWidget.builder(Text.translatable("immersivechess.offer_draw").styled(s -> s.withColor(Formatting.YELLOW)), createButtonAction(ChessGameScreenHandler.Button.DRAW))
            .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();

    private final ButtonWidget resignButton = ButtonWidget.builder(Text.translatable("immersivechess.resign").styled(s -> s.withColor(Formatting.YELLOW)), createButtonAction(ChessGameScreenHandler.Button.RESIGN))
            .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();

    private final ButtonWidget endButton = ButtonWidget.builder(Text.translatable("immersivechess.clear_board").styled(s -> s.withColor(Formatting.RED)), createButtonAction(ChessGameScreenHandler.Button.STOP_BOARD))
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.of(Text.translatable("immersivechess.clear_board.tooltip")))
            .build();

    private final ButtonWidget closeButton = ButtonWidget.builder(Text.translatable("menu.returnToGame"), b -> close())
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();

    private final CyclingButtonWidget<PieceRenderOption> whitePieceButton;
    private final CyclingButtonWidget<PieceRenderOption> blackPieceButton;

    public ChessGameScreen(ChessGameScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 245;

        whitePieceButton = CyclingButtonWidget.builder(PieceRenderOption::getText)
                .values(handler.getWhiteRenderOptions())
                .initially(handler.getInitialWhiteRenderOption())
                .tooltip(option -> Tooltip.of(Text.translatable("immersivechess.white_piece_render_option_button.tooltip")))
                .build(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("immersivechess.piece_render_option_button"), createPieceRenderOptionAction(Color.WHITE));

        blackPieceButton = CyclingButtonWidget.builder(PieceRenderOption::getText)
                .values(handler.getBlackRenderOptions())
                .initially(handler.getInitialBlackRenderOption())
                .tooltip(option -> Tooltip.of(Text.translatable("immersivechess.black_piece_render_option_button.tooltip")))
                .build(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("immersivechess.piece_render_option_button"), createPieceRenderOptionAction(Color.BLACK));
    }

    @Override
    protected void init() {
        super.init();

        if (client == null || client.player == null) return;
        PlayerEntity player = client.player;

        int leftX = x + backgroundWidth / 4 - BUTTON_WIDTH / 2;
        int rightX = x + 3 * backgroundWidth / 4 - BUTTON_WIDTH / 2;

        int buttonY = y + 55;
        int buttonDist = BUTTON_HEIGHT + BUTTON_PADDING;

        // render options: only if current player or OP
        if (player.isCreativeLevelTwoOp() || player.getEntityName().equals(handler.getWhite()) || player.getEntityName().equals(handler.getBlack())){
            if (!handler.getWhite().isEmpty())
                addDrawableChild(DataFixUtils.make(whitePieceButton, b -> b.setPosition(leftX, buttonY)));
            if (!handler.getBlack().isEmpty())
                addDrawableChild(DataFixUtils.make(blackPieceButton, b -> b.setPosition(rightX, buttonY)));
        }

        // game controls: only when game valid and not finished
        if (!handler.getStatus().isFinished() && handler.getWhite().length() != 0 && handler.getBlack().length() != 0) {
            // only for players of the game
            if (player.getEntityName().equals(handler.getWhite()) || player.getEntityName().equals(handler.getBlack())) {

                // set buttons on correct side
                Color playerColor = player.getEntityName().equals(handler.getWhite()) ? Color.WHITE : Color.BLACK;
                int resignX = switch(playerColor) {case WHITE -> leftX; case BLACK -> rightX;};
                int drawX = switch(playerColor) {case WHITE -> rightX; case BLACK -> leftX;};

                if (player.getEntityName().equals(handler.getDrawOfferedTo())) {
                    drawButton.setMessage(Text.translatable("immersivechess.accept_draw").styled(s -> s.withColor(Formatting.YELLOW)));
                    drawButton.setTooltip(Tooltip.of(Text.translatable("immersivechess.accept_draw.tooltip")));
                }

                addDrawableChild(DataFixUtils.make(drawButton, b -> b.setPosition(drawX, buttonY + buttonDist)));
                addDrawableChild(DataFixUtils.make(resignButton, b -> b.setPosition(resignX, buttonY + buttonDist)));
            }
        }

        // cleanup board: only when game finished, not started or for OP
        if (handler.getStatus().isFinished() || player.isCreativeLevelTwoOp() || handler.getMoveCount() == 1)
            addDrawableChild(DataFixUtils.make(endButton, b -> b.setPosition(leftX, buttonY + 3 * buttonDist)));

        // back to game button
        addDrawableChild(DataFixUtils.make(closeButton, b -> b.setPosition(rightX, buttonY + 3 * buttonDist)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // grays out world
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        super.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // player names
        float nameY = titleY + 16;
        float whiteX = backgroundWidth / 4f - textRenderer.getWidth(handler.getWhite()) / 2f;
        float blackX = 3 * backgroundWidth / 4f - textRenderer.getWidth(handler.getBlack()) / 2f;
        this.textRenderer.draw(matrices, handler.getWhite(), whiteX, nameY, 0xFFFFFF);
        this.textRenderer.draw(matrices, handler.getBlack(), blackX, nameY, 0x000000);

        // move counter
        float moveY = nameY + 16;
        String moveCountText = Integer.toString((int) Math.ceil(handler.getMoveCount() / 2d));
        int moveCountWidth = textRenderer.getWidth(moveCountText);
        float moveX = backgroundWidth / 2f - moveCountWidth / 2f;
        this.textRenderer.draw(matrices, moveCountText, moveX, moveY, 0x404040);

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // turn and game outcome
        switch (handler.getStatus()) {
            case NOT_FINISHED -> {
                // turn
                switch (handler.getTurn()) {
                    case WHITE -> drawTexture(matrices, (int) moveX - 7, (int) moveY + 1, 0, backgroundHeight, 4, 6);
                    case BLACK ->
                            drawTexture(matrices, (int) moveX + 3 + moveCountWidth, (int) moveY + 1, 4, backgroundHeight, 4, 6);
                }
            }
            // winner
            case WIN_WHITE ->
                    drawTexture(matrices, backgroundWidth / 4 - 5, (int) nameY - 9, 8, backgroundHeight, 11, 5);
            case WIN_BLACK ->
                    drawTexture(matrices, 3 * backgroundWidth / 4 - 5, (int) nameY - 9, 8, backgroundHeight, 11, 5);
            // draw
            case DRAW, DRAW_STALEMATE, DRAW_REPETITION, DRAW_NOCAPTURE -> {
                drawTexture(matrices, backgroundWidth / 4 - 5, (int) nameY - 9, 19, backgroundHeight, 11, 5);
                drawTexture(matrices, 3 * backgroundWidth / 4 - 5, (int) nameY - 9, 19, backgroundHeight, 11, 5);
            }
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    private ButtonWidget.PressAction createButtonAction(ChessGameScreenHandler.Button buttonType) {
        return button -> {
            this.sendButtonPressPacket(buttonType.ordinal());
            close();
        };
    }

    private CyclingButtonWidget.UpdateCallback<PieceRenderOption> createPieceRenderOptionAction(Color color) {
        return (button, value) -> {
            this.sendButtonPressPacket(ChessGameScreenHandler.Button.getRenderOptionButton(color, value).ordinal());
        };
    }

    private void sendButtonPressPacket(int id) {
        if (client == null || client.interactionManager == null) return;
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

}
