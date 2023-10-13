package be.immersivechess.advancement.criterion;

import net.minecraft.advancement.criterion.Criterion;

public class Criteria {

    public static ChessGameCriterion GAME_CRITERION = register(new ChessGameCriterion());

    private static <T extends Criterion<?>> T register(T c) {
        return net.minecraft.advancement.criterion.Criteria.register(c);
    }

    public static void onInitialize() {

    }
}
