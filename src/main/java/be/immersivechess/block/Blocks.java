package be.immersivechess.block;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.logic.Piece;
import ch.astorm.jchess.core.Color;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.*;
import java.util.stream.Collectors;

public class Blocks {

    public static final SortedMap<Piece, PieceBlock> PIECES = Arrays.stream(Piece.values()).collect(Collectors.toMap(
            p -> p,
            Blocks::createPieceBlock,
            (v1,v2) ->{ throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));},
            TreeMap::new)
    );

    public static final StandBlock BLANK_STAND = register(
            new Identifier(ImmersiveChess.MOD_ID, "stand"),
            new StandBlock(FabricBlockSettings
                    .create()
                    .mapColor(MapColor.GOLD)
                    .pistonBehavior(PistonBehavior.DESTROY)
                    .strength(0.3f)
                    .nonOpaque()));

    public static final List<PieceStandBlock> PIECE_STANDS = Arrays.stream(Piece.values()).map(Blocks::createPieceStandBlock).toList();

    public static final List<PieceStructureBlock> PIECE_STRUCTURE_BLOCKS = Arrays.stream(Piece.values()).map(Blocks::createPieceStructureBlock).toList();

    public static BoardBlock BOARD_BLOCK = register("board", new BoardBlock(FabricBlockSettings
            .create()
            .strength(-1.0f, 3600000.0f)
            .nonOpaque()
            .dynamicBounds()        // to disable shape cache -> for overriding opacity
            .noBlockBreakParticles()
            .dropsNothing()
            .allowsSpawning(Blocks::never)
    ));

    private static PieceBlock createPieceBlock(Piece piece) {
        return register(piece.getIdentifier().withPrefixedPath("piece/"), new PieceBlock(
                FabricBlockSettings
                        .create()
                        .mapColor(MapColor.CLEAR)
                        .pistonBehavior(PistonBehavior.BLOCK)
                        .hardness(0f)
                        .resistance(3600000.0f)
                        .nonOpaque()
                        .noBlockBreakParticles(),
                piece)
        );
    }

    private static PieceStandBlock createPieceStandBlock(Piece piece) {
        return register(piece.getIdentifier().withPrefixedPath("stand/"), new PieceStandBlock(
                FabricBlockSettings
                        .create()
                        .mapColor(piece.getColor() == Color.WHITE ? MapColor.WHITE : MapColor.BLACK)
                        .pistonBehavior(PistonBehavior.DESTROY)
                        .strength(0.3f)
                        .nonOpaque(),
                piece)
        );
    }

    private static PieceStructureBlock createPieceStructureBlock(Piece piece) {
        return register(piece.getIdentifier().withPrefixedPath("structure/"), new PieceStructureBlock(
                FabricBlockSettings
                        .create()
                        .strength(3f)
                        .dynamicBounds(),
                piece));
    }

    private static <A> boolean never(BlockState var1, BlockView var2, BlockPos var3, A var4) {
        return false;
    }

    private static <B extends Block> B register(Identifier id, B block) {
        return Registry.register(Registries.BLOCK, id, block);
    }

    private static <B extends Block> B register(String name, B block) {
        return register(new Identifier(ImmersiveChess.MOD_ID, name), block);
    }

    public static void onInitialize() {

    }
}
