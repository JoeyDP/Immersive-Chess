package be.immersivechess.advancement.criterion;

import be.immersivechess.ImmersiveChess;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.util.Identifier;

public class Criteria {

    public static ChessGameCriterion GAME_CRITERION = register(new Identifier(ImmersiveChess.MOD_ID, "game_trigger"), new ChessGameCriterion());

    private static <T extends Criterion<?>> T register(Identifier id, T c) {
        return net.minecraft.advancement.criterion.Criteria.register(id.toString(), c);
    }

    public static void onInitialize() {

    }
}
