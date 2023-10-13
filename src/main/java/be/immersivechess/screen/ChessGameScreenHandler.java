package be.immersivechess.screen;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.world.ChessGameState;
import be.immersivechess.world.PieceRenderOption;
import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.core.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChessGameScreenHandler extends ScreenHandler {

    public enum Button {
        // Game controls
        DRAW,   // can be offer or accept
        RESIGN,
        STOP_BOARD,

        // render options
        RENDER_WHITE_DEFAULT(Color.WHITE, PieceRenderOption.DEFAULT),
        RENDER_WHITE_OWN(Color.WHITE, PieceRenderOption.OWN),
        RENDER_WHITE_OPPONENT(Color.WHITE, PieceRenderOption.OPPONENT),

        RENDER_BLACK_DEFAULT(Color.BLACK, PieceRenderOption.DEFAULT),
        RENDER_BLACK_OWN(Color.BLACK, PieceRenderOption.OWN),
        RENDER_BLACK_OPPONENT(Color.BLACK, PieceRenderOption.OPPONENT);

        Button() {

        }

        @Nullable
        private Color color;
        @Nullable
        private PieceRenderOption option;

        Button(@Nullable Color color, @Nullable PieceRenderOption option) {
            this.color = color;
            this.option = option;
        }

        public static Button getRenderOptionButton(Color color, PieceRenderOption option) {
            if (color == null || option == null) return null;
            return renderOptionMap.get(color).get(option);
        }

        private static final Map<Color, Map<PieceRenderOption, Button>> renderOptionMap = Map.of(
                Color.WHITE, Map.of(
                        PieceRenderOption.DEFAULT, RENDER_WHITE_DEFAULT,
                        PieceRenderOption.OWN, RENDER_WHITE_OWN,
                        PieceRenderOption.OPPONENT, RENDER_WHITE_OPPONENT
                ),
                Color.BLACK, Map.of(
                        PieceRenderOption.DEFAULT, RENDER_BLACK_DEFAULT,
                        PieceRenderOption.OWN, RENDER_BLACK_OWN,
                        PieceRenderOption.OPPONENT, RENDER_BLACK_OPPONENT
                )
        );
    }

    // Client side
    @Environment(EnvType.CLIENT)
    private String white;
    @Environment(EnvType.CLIENT)
    private String black;
    @Environment(EnvType.CLIENT)
    private int moveCount;
    @Environment(EnvType.CLIENT)
    private Color turn;
    @Environment(EnvType.CLIENT)
    private JChessGame.Status status;
    @Environment(EnvType.CLIENT)
    private String drawOfferedTo;
    @Environment(EnvType.CLIENT)
    private List<PieceRenderOption> whiteRenderOptions;
    @Environment(EnvType.CLIENT)
    private List<PieceRenderOption> blackRenderOptions;
    @Environment(EnvType.CLIENT)
    private PieceRenderOption initialWhiteRenderOption;
    @Environment(EnvType.CLIENT)
    private PieceRenderOption initialBlackRenderOption;

    // Called on client
    public ChessGameScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packetByteBuf) {
        super(ScreenHandlerTypes.CHESS_GAME_SCREEN_HANDLER_TYPE, syncId);

        // See handler creation in `BoardBlock` for packet construction
        white = packetByteBuf.readString();
        black = packetByteBuf.readString();
        moveCount = packetByteBuf.readInt();
        turn = packetByteBuf.readEnumConstant(Color.class);
        status = packetByteBuf.readEnumConstant(JChessGame.Status.class);
        drawOfferedTo = packetByteBuf.readString();
        whiteRenderOptions = Arrays.stream(packetByteBuf.readIntArray()).mapToObj(PieceRenderOption::get).toList();
        blackRenderOptions = Arrays.stream(packetByteBuf.readIntArray()).mapToObj(PieceRenderOption::get).toList();
        initialWhiteRenderOption = packetByteBuf.readEnumConstant(PieceRenderOption.class);
        initialBlackRenderOption = packetByteBuf.readEnumConstant(PieceRenderOption.class);
    }

    @Environment(EnvType.CLIENT)
    public String getBlack() {
        return black;
    }

    @Environment(EnvType.CLIENT)
    public String getWhite() {
        return white;
    }

    @Environment(EnvType.CLIENT)
    public int getMoveCount() {
        return moveCount;
    }

    @Environment(EnvType.CLIENT)
    public Color getTurn() {
        return turn;
    }

    @Environment(EnvType.CLIENT)
    public JChessGame.Status getStatus() {
        return status;
    }

    @Environment(EnvType.CLIENT)
    public String getDrawOfferedTo() {
        return drawOfferedTo;
    }

    @Environment(EnvType.CLIENT)
    public List<PieceRenderOption> getWhiteRenderOptions() {
        return whiteRenderOptions;
    }

    @Environment(EnvType.CLIENT)
    public List<PieceRenderOption> getBlackRenderOptions() {
        return blackRenderOptions;
    }

    @Environment(EnvType.CLIENT)
    public PieceRenderOption getInitialWhiteRenderOption() {
        return initialWhiteRenderOption;
    }

    @Environment(EnvType.CLIENT)
    public PieceRenderOption getInitialBlackRenderOption() {
        return initialBlackRenderOption;
    }

    // Server side
    private ChessGameState gameState;

    // Called on server
    public ChessGameScreenHandler(int syncId, @Nullable ChessGameState gameState) {
        super(ScreenHandlerTypes.CHESS_GAME_SCREEN_HANDLER_TYPE, syncId);
        this.gameState = gameState;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id >= Button.values().length)
            return false;

        if (gameState == null)
            return false;

        Button button = Button.values()[id];
        return switch (button) {
            case DRAW -> drawAction(player);
            case RESIGN -> resignAction(player);
            case STOP_BOARD -> endBoardBlocks(player);
            case RENDER_WHITE_DEFAULT, RENDER_WHITE_OWN, RENDER_WHITE_OPPONENT,
                    RENDER_BLACK_DEFAULT, RENDER_BLACK_OWN, RENDER_BLACK_OPPONENT
                    -> setRenderOption(player, button.color, button.option);
        };
    }

    private boolean drawAction(PlayerEntity player) {
        gameState.draw(player);
        return false;
    }

    private boolean resignAction(PlayerEntity player) {
        return gameState.resign(player);
    }

    private boolean endBoardBlocks(PlayerEntity player) {
        // Op can force a draw to end the game
        if (!gameState.getStatus().isFinished() && player.isCreativeLevelTwoOp())
            gameState.forceDraw();

        // Only allow this action when game is finished or not started
        if (!(gameState.getStatus().isFinished() || gameState.getCurrentMoveIndex() == 1))
            return false;

        gameState.endBoardBlocks();
        return true;
    }

    private boolean setRenderOption(PlayerEntity player, Color color, PieceRenderOption option) {
        if (!(player.isCreativeLevelTwoOp() || player.getEntityName().equals(gameState.getPlayerName(Color.WHITE)) || player.getEntityName().equals(gameState.getPlayerName(Color.BLACK))))
            return false;

        return gameState.setRenderOption(color, option);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

}
