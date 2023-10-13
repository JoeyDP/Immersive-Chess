package be.immersivechess.screen;

import be.immersivechess.ImmersiveChess;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ScreenHandlerTypes {
    public static final ScreenHandlerType<ChessCaseScreenHandler> CHESS_CASE_SCREEN_HANDLER_TYPE = register("chess_case", ChessCaseScreenHandler::new);
    public static final ScreenHandlerType<ChessGameScreenHandler> CHESS_GAME_SCREEN_HANDLER_TYPE = registerExtended("chess_game", ChessGameScreenHandler::new);

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String id, ScreenHandlerType.Factory<T> factory) {
        return Registry.register(Registries.SCREEN_HANDLER, new Identifier(ImmersiveChess.MOD_ID, id), new ScreenHandlerType<>(factory, FeatureSet.empty()));
    }

    private static <T extends ScreenHandler> ScreenHandlerType<T> registerExtended(String id, ExtendedScreenHandlerType.ExtendedFactory<T> factory) {
        return Registry.register(Registries.SCREEN_HANDLER, new Identifier(ImmersiveChess.MOD_ID, id), new ExtendedScreenHandlerType<>(factory));
    }

    public static void onInitialize(){

    }

}
