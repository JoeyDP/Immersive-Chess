package be.immersivechess.world;

import ch.astorm.jchess.core.Color;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public enum PieceRenderOption {
    DEFAULT,
    OWN,
    OPPONENT;

    public Text getText() {
        return Text.translatable("immersivechess.piece_render_option." + toString().toLowerCase());
    }

    public static PieceRenderOption get(int index) {
        if (index < 0 || index > PieceRenderOption.values().length)
            throw new IllegalArgumentException();

        return PieceRenderOption.values()[index];
    }

    public static Map<Color, PieceRenderOption> createDefaultRenderOptions(){
        Map<Color, PieceRenderOption> ret = new HashMap<>();
        ret.put(Color.WHITE, DEFAULT);
        ret.put(Color.BLACK, DEFAULT);
        return ret;
    }
}
