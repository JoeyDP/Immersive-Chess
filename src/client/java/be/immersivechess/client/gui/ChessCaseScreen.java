package be.immersivechess.client.gui;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.screen.ChessCaseScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ChessCaseScreen extends HandledScreen<ChessCaseScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ImmersiveChess.MOD_ID, "textures/gui/container/chess_case.png");

    public ChessCaseScreen(ChessCaseScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // grays out world
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

}
