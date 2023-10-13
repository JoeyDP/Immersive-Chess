package be.immersivechess.logic;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.PieceBlock;
import ch.astorm.jchess.core.Color;
import ch.astorm.jchess.core.Moveable;
import ch.astorm.jchess.core.entities.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Piece {
    // NOTE: careful! values are iterated in ChessCaseScreenHandler. Order is important!
    BLACK_PAWN("black_pawn", 10, Color.BLACK, Pawn.class){
        @Override
        public List<Piece> getPromotions() {
            return List.of(BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN);
        }
    },
    BLACK_ROOK ("black_rook", 12, Color.BLACK, Rook.class),
    BLACK_KNIGHT ("black_knight", 13, Color.BLACK, Knight.class),
    BLACK_BISHOP ("black_bishop", 14, Color.BLACK, Bishop.class),
    BLACK_QUEEN ("black_queen", 15, Color.BLACK, Queen.class),
    BLACK_KING ("black_king", 16, Color.BLACK, King.class),

    WHITE_PAWN("white_pawn", 10, Color.WHITE, Pawn.class){
        @Override
        public List<Piece> getPromotions() {
            return List.of(WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN);
        }
    },
    WHITE_ROOK ("white_rook", 12, Color.WHITE, Rook.class),
    WHITE_KNIGHT ("white_knight", 13, Color.WHITE, Knight.class),
    WHITE_BISHOP ("white_bishop", 14, Color.WHITE, Bishop.class),
    WHITE_QUEEN ("white_queen", 15, Color.WHITE, Queen.class),
    WHITE_KING ("white_king", 16, Color.WHITE, King.class);

    private final Identifier identifier;
    // Height in 1/8 of a block for designing the piece (base is 8x8)
    private final int height;
    private final Color color;
    private final Class<? extends Moveable> libClass;

    Piece(String name, int height, Color color, Class<? extends Moveable> libClass) {
        this.identifier = new Identifier(ImmersiveChess.MOD_ID, name);
        this.height = height;
        this.color = color;
        this.libClass = libClass;
    }

    public static Piece fromName(String name) {
        return Piece.valueOf(name.toUpperCase());
    }

    public static Piece fromMoveable(Moveable moveable) {
        if (moveable == null) return null;
        List<Piece> candidates = Arrays.stream(Piece.values()).filter(p -> p.color == moveable.getColor() && p.libClass.isInstance(moveable)).toList();
        if (candidates.size() == 1) return candidates.get(0);
        throw new IllegalArgumentException("Failed to convert Moveable to Piece: " + moveable);
    }

    public Moveable createMoveable(){
        try {
            return libClass.getDeclaredConstructor(Color.class).newInstance(color);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Piece> getPromotions(){
        return Collections.emptyList();
    }

    public Identifier getIdentifier(){
        return identifier;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor(){
        return color;
    }

    public PieceBlock getBlock(){
        return Blocks.PIECES.get(this);
    }

    public BlockState getBlockState(Direction facing){
        return getBlock().getDefaultState().with(PieceBlock.FACING, facing);
    }


    public Identifier getDefaultStructureIdentifier() {
        return getIdentifier();
    }

}
