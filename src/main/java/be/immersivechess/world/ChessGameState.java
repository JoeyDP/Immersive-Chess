package be.immersivechess.world;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.advancement.criterion.ChessGameCriterion;
import be.immersivechess.advancement.criterion.Criteria;
import be.immersivechess.block.BoardBlock;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.item.PieceItem;
import be.immersivechess.logic.MultiblockBoard;
import be.immersivechess.logic.Piece;
import be.immersivechess.structure.StructureMap;
import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.core.*;
import ch.astorm.jchess.core.entities.King;
import ch.astorm.jchess.core.rules.Displacement;
import ch.astorm.jchess.io.PGNReader;
import ch.astorm.jchess.io.PGNWriter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChessGameState extends PersistentState {
    // constants
    private static final String LOCATION = ImmersiveChess.MOD_ID + "/games/";

    // Nbt keys
    private static final String GAME_KEY = "Game";
    private static final String BOARD_KEY = "Board";
    private static final String MINED_KEY = "Mined";
    private static final String DRAW_OFFER_KEY = "DrawOfferedBy";
    private static final String WHITE_PLAYER_STRUCTURES_KEY = "StructuresOfWhite";
    private static final String BLACK_PLAYER_STRUCTURES_KEY = "StructuresOfBlack";
    private static final String BLACK_PIECES_RENDER_OPTION_KEY = "BlackRenderOption";
    private static final String WHITE_PIECES_RENDER_OPTION_KEY = "WhiteRenderOption";

    // pgn metadata keys
    private static final String GAME_ID_KEY = "GameId";
    private static final String WHITE_PLAYER_KEY = "White";
    private static final String BLACK_PLAYER_KEY = "Black";
    private static final String DATE_KEY = "Date";

    private final ServerWorld world;

    // State that is persisted in nbt
    private final JChessGame game;
    private final MultiblockBoard board;
    private Coordinate minedSquare;

    // contains all structures (both colors) for each player. In total mapping contains two sets of white and two sets of black pieces.
    private final Map<Color, StructureMap> playerStructures = new HashMap<>();
    private final Map<Color, PieceRenderOption> renderOptions = PieceRenderOption.createDefaultRenderOptions();

    private Color drawOfferedBy;

    private ChessGameState(ServerWorld world, JChessGame game, MultiblockBoard board) {
        this(world, game, board, null, null);
    }

    private ChessGameState(ServerWorld world, JChessGame game, MultiblockBoard board, Coordinate minedSquare, Color DrawOfferedBy) {
        this.world = world;
        this.game = game;
        this.board = board;
        this.minedSquare = minedSquare;
        this.drawOfferedBy = DrawOfferedBy;
    }

    public static ChessGameState create(ServerWorld world, MultiblockBoard board) {
        ChessGameState state = new ChessGameState(world, JChessGame.newGame(), board);
        state.game.getMetadata().put(GAME_ID_KEY, UUID.randomUUID().toString());
        state.game.getMetadata().put(DATE_KEY, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        state.markDirty();

        world.getPersistentStateManager().set(state.getSavePath(), state);

        return state;
    }

    @Nullable
    public static ChessGameState get(ServerWorld world, String gameId) {
        Type<ChessGameState> type = new Type<>(null, nbt -> fromNbt(world, nbt), null);
        return world.getPersistentStateManager().get(type, LOCATION + gameId);
    }

    public String getGameId() {
        return game.getMetadata().get(GAME_ID_KEY);
    }

    public Direction getWhitePlayDirection() {
        return board.getWhitePlayDirection();
    }

    private void setPlayerName(Color color, @Nullable String name) {
        String key = color == Color.WHITE ? WHITE_PLAYER_KEY : BLACK_PLAYER_KEY;
        if (name != null)
            game.getMetadata().put(key, name);
        else
            game.getMetadata().remove(key);
        markDirty();
    }

    public void setPlayer(Color color, PlayerEntity mcPlayer) {
        setPlayerName(color, mcPlayer.getEntityName());
        if (mcPlayer instanceof ServerPlayerEntity serverPlayer)
            Criteria.GAME_CRITERION.trigger(serverPlayer, ChessGameCriterion.Type.START);
    }

    public void setPlayer(Color color, PlayerEntity mcPlayer, StructureMap structures) {
        setPlayer(color, mcPlayer);
        setStructures(color, structures);
    }

    public void removePlayer(Color color) {
        setPlayerName(color, null);
        playerStructures.remove(color);
        setRenderOption(color, PieceRenderOption.DEFAULT);


        // also removes when player is not present
        placePieces();
    }

    public boolean togglePlayer(Color color, PlayerEntity mcPlayer, StructureMap structures) {
        String playerName = getPlayerName(color);
        if (playerName == null) {
            setPlayer(color, mcPlayer, structures);
            return true;
        }
        if (playerName.equals(mcPlayer.getEntityName())) {
            removePlayer(color);
            return true;
        }
        return false;
    }

    @Nullable
    public String getPlayerName(Color color) {
        String name = game.getMetadata().get(color == Color.WHITE ? WHITE_PLAYER_KEY : BLACK_PLAYER_KEY);
        if (name == null || name.isEmpty()) return null;
        return name;
    }

    public Color getColor(PlayerEntity player) {
        for (Color color : Color.values())
            if (player.getEntityName().equals(getPlayerName(color))) return color;
        return null;
    }

    public Color getColorOnMove() {
        return game.getColorOnMove();
    }

    public int getCurrentMoveIndex() {
        return game.getPosition().getMoveHistory().size() + 1;
    }

    public MultiblockBoard getBoard() {
        return board;
    }

    public boolean hasPlayerMinedPiece() {
        return minedSquare != null;
    }

    @Nullable
    public String getActivePlayerName() {
        Color activeColor = game.getColorOnMove();
        return getPlayerName(activeColor);
    }

    @Nullable
    public Piece getPiece(Coordinate square) {
        Moveable moveable = game.getPosition().get(square);
        if (moveable == null) return null;
        return Piece.fromMoveable(moveable);
    }

    public StructureMap getStructuresOfPlayer(Color color) {
        return playerStructures.computeIfAbsent(color, c -> new StructureMap());
    }

    public void setStructures(Color color, StructureMap structureMap) {
        playerStructures.put(color, structureMap);
        if (structureMap.hasAnyOf(color))
            setRenderOption(color, PieceRenderOption.OWN);

        placePieces();
        markDirty();
    }

    public PieceRenderOption getRenderOption(Color color) {
        return renderOptions.get(color);
    }

    public List<PieceRenderOption> getValidRenderOptions(Color color) {
        List<PieceRenderOption> list = new ArrayList<>();
        list.add(PieceRenderOption.DEFAULT);
        if (playerStructures.get(color) != null && playerStructures.get(color).hasAnyOf(color))
            list.add(PieceRenderOption.OWN);
        if (playerStructures.get(color.opposite()) != null && playerStructures.get(color.opposite()).hasAnyOf(color))
            list.add(PieceRenderOption.OPPONENT);
        return list;
    }

    public boolean setRenderOption(Color color, PieceRenderOption option) {
        if (!getValidRenderOptions(color).contains(option)) return false;
        renderOptions.put(color, option);
        markDirty();
        updatePieceStructures();
        return true;
    }

    private void updatePieceStructures() {
        for (Coordinate square : game.getPosition().getMoveables().keySet()) {
            updatePieceStructure(square);
        }
    }

    private void updatePieceStructure(Coordinate square) {
        BlockPos destPos = board.getPos(square).up();
        Piece piece = getPiece(square);
        if (piece == null) return;

        BlockEntity blockEntity = world.getBlockEntity(destPos);
        if (blockEntity instanceof StructureRenderedBlockEntity pieceBlockEntity) {
            pieceBlockEntity.setStructure(getStructure(piece));
        }
    }

    @Nullable
    public NbtCompound getStructure(Piece piece) {
        StructureMap defaultStructures = StructureMap.getDefault(world);
        return switch (getRenderOption(piece.getColor())) {
            case DEFAULT -> defaultStructures.get(piece);
            case OWN -> playerStructures.get(piece.getColor()).getOrDefault(piece, defaultStructures.get(piece));
            case OPPONENT ->
                    playerStructures.get(piece.getColor().opposite()).getOrDefault(piece, defaultStructures.get(piece));
        };
    }

    public boolean isPlayerOnMove(PlayerEntity player) {
        return player.getEntityName().equals(getActivePlayerName());
    }

    public JChessGame.Status getStatus() {
        return game.getStatus();
    }

    public boolean resign(PlayerEntity player) {
        if (game.getStatus().isFinished()) return false;

        // game not ready yet
        if (getPlayerName(Color.WHITE) == null || getPlayerName(Color.BLACK) == null) return false;

        Color color = getColor(player);
        if (color == null) return false;
        game.resign(color);
        markDirty();
        onGameEnded();
        return true;
    }

    /**
     * Offers or accepts draw by player depending on state.
     */
    public boolean draw(PlayerEntity player) {
        if (game.getStatus().isFinished()) return false;

        // game not ready yet
        if (getPlayerName(Color.WHITE) == null || getPlayerName(Color.BLACK) == null) return false;

        Color playerColor = getColor(player);
        if (playerColor == null) return false;

        // offer draw
        if (drawOfferedBy == null) {
            drawOfferedBy = playerColor;
            markDirty();

            ServerPlayerEntity opponent = getPlayer(drawOfferedBy.opposite());
            if (opponent != null) {
                opponent.sendMessage(Text.translatable("immersivechess.draw_offer_message", getPlayerName(drawOfferedBy)), true);
                opponent.sendMessage(Text.translatable("immersivechess.draw_offer_chat", getPlayerName(drawOfferedBy)), false);
                opponent.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1f, 1f);
            }
            return true;
        }

        // accept draw
        if (playerColor.equals(drawOfferedBy.opposite()) || getPlayerName(Color.WHITE).equals(getPlayerName(Color.BLACK))) {
            game.draw();
            markDirty();
            onGameEnded();
            return true;
        }

        return false;
    }

    /**
     * Force draw if game not already finished.
     */
    public void forceDraw() {
        if (game.getStatus().isFinished()) return;
        game.draw();
        onGameEnded();
    }

    public String getDrawOfferedTo() {
        if (drawOfferedBy == null)
            return "";
        String opponent = getPlayerName(drawOfferedBy.opposite());
        return opponent != null ? opponent : "";
    }

    private void onGameEnded() {
        switch (game.getStatus()) {
            case NOT_FINISHED -> {
                ImmersiveChess.LOGGER.error("Invalid call to onGameEnded when status is NOT_FINISHED");
            }
            case WIN_WHITE -> {
                onGameEnded(Color.WHITE);
            }
            case WIN_BLACK -> {
                onGameEnded(Color.BLACK);
            }
            case DRAW, DRAW_STALEMATE, DRAW_REPETITION, DRAW_NOCAPTURE -> {
                ServerPlayerEntity p1 = getPlayer(Color.WHITE);
                ServerPlayerEntity p2 = getPlayer(Color.BLACK);

                if (p1 != null)
                    p1.sendMessage(Text.translatable("immersivechess.draw_message"), true);
                if (p2 != null)
                    p2.sendMessage(Text.translatable("immersivechess.draw_message"), true);

                String p1Name = getPlayerName(Color.WHITE);
                String p2Name = getPlayerName(Color.BLACK);
                if (p1Name != null && !p1Name.equals(p2Name))
                    world.getServer().getPlayerManager().broadcast(Text.translatable("immersivechess.draw_broadcast", p1Name, p2Name), false);
                world.playSound(null, board.getPos(new Coordinate("a1")), SoundEvents.GOAT_HORN_SOUNDS.get(0).value(), SoundCategory.PLAYERS, 1f, 1f);
            }
        }
    }

    private void onGameEnded(Color winColor) {
        ServerPlayerEntity winner = getPlayer(winColor);
        ServerPlayerEntity loser = getPlayer(winColor.opposite());

        // advancements
        Criteria.GAME_CRITERION.trigger(winner, ChessGameCriterion.Type.WIN);
        Criteria.GAME_CRITERION.trigger(loser, ChessGameCriterion.Type.LOSE);

        // announce in chat to all
        String winnerName = getPlayerName(winColor);
        String loserName = getPlayerName(winColor.opposite());
        if (winnerName != null && !winnerName.equals(loserName))
            world.getServer().getPlayerManager().broadcast(Text.translatable("immersivechess.win_broadcast", winnerName, loserName), false);

        // hover text to players
        if (winner != null)
            winner.sendMessage(Text.translatable("immersivechess.win_message"), true);
        if (loser != null && !loser.equals(winner))
            loser.sendMessage(Text.translatable("immersivechess.lose_message"), true);

        // play victory sound for all except loser
        BlockPos pos = board.getPos(new Coordinate("a1"));
        world.playSound(Objects.equals(loser, winner) ? null : loser, pos, SoundEvents.GOAT_HORN_SOUNDS.get(0).value(), SoundCategory.PLAYERS, 1f, 1.5f);
        if (loser != null && !loser.equals(winner))
            loser.playSound(SoundEvents.GOAT_HORN_SOUNDS.get(7).value(), SoundCategory.PLAYERS, 1f, 0.8f);
    }

    /**
     * Checks if the active player still has at least a piece in their inventory and if not, places the mined block back.
     * Also updates world with logical board.
     */
    public void performIntegrityCheck() {
        PlayerEntity player = getActivePlayer();
        if (minedSquare != null && player != null && !player.getInventory().contains(PieceItem.PIECE_TAG))
            clearMinedSquare();

        placePieces();
    }

    public void placePieces() {
        for (Map.Entry<Coordinate, Moveable> entry : game.getPosition().getMoveables().entrySet()) {
            if (entry.getKey().equals(minedSquare))
                continue;

            if (getPlayerName(entry.getValue().getColor()) == null) {
                breakPiece(entry.getKey());
            } else {
                Piece piece = Piece.fromMoveable(entry.getValue());
                placePiece(entry.getKey(), piece);
            }
        }
    }

    @Nullable
    private ServerPlayerEntity findPlayer(String name) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getEntityName().equals(name))
                return player;
        }
        return null;
    }

    @Nullable
    private ServerPlayerEntity getPlayer(Color color) {
        return findPlayer(getPlayerName(color));
    }

    @Nullable
    private ServerPlayerEntity getActivePlayer() {
        return findPlayer(getActivePlayerName());

    }

    /**
     * Performs the cleanup after a game has ended
     */
    public void endBoardBlocks() {
        BlockBox bounds = board.getBounds();
        for (BlockPos blockPos : BlockPos.iterate(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ())) {
            BoardBlock.placeBack(world, blockPos);
        }
    }

    public boolean canMinePiece(Coordinate square, PlayerEntity player) {
        if (hasPlayerMinedPiece()) return false;

        // game not ready yet
        if (getPlayerName(Color.WHITE) == null || getPlayerName(Color.BLACK) == null) return false;

        if (game.getStatus().isFinished()) return false;

        if (game.getPosition().get(square) == null) return false;

        Color activeColor = game.getColorOnMove();
        if (game.getPosition().get(square).getColor() != activeColor) return false;

        String activePlayer = getActivePlayerName();
        if (activePlayer == null) return false;

        return activePlayer.equals(player.getEntityName());
    }

    public void setMinedSquare(Coordinate square) {
        if (hasPlayerMinedPiece())
            ImmersiveChess.LOGGER.error("Trying to set piece mined when there already was one mined");
        minedSquare = square;
        markDirty();

        // also set correct piece to BoardBlockEntity for rendering
        if (world.getBlockEntity(board.getPos(square)) instanceof BoardBlockEntity boardBlockEntity) {
            boardBlockEntity.setPiece(getPiece(square));
        }
    }

    private void clearMinedSquare() {
        // also clear piece of BoardBlockEntity for rendering
        if (minedSquare != null && world.getBlockEntity(board.getPos(minedSquare)) instanceof BoardBlockEntity boardBlockEntity) {
            boardBlockEntity.setPiece(null);
        }

        minedSquare = null;
        markDirty();
    }

    public void setMinedSquare(BlockPos pos) {
        Coordinate square = board.getSquare(pos.down());
        if (square == null) {
            ImmersiveChess.LOGGER.error("Failed to convert world pos to chess square " + pos);
            return;
        }
        setMinedSquare(square);
    }


    private Displacement createDisplacement(Coordinate source, Coordinate destination) {
        // if not a promotion -> do not provide piece
        Moveable sourcePiece = game.getPosition().get(source);
        return new Displacement(sourcePiece, source, destination);
    }

    /**
     * This is the main way in which legal moves are checked because it can handle the case of pawn promotion.
     * With pawn promotion some pieces are not allowed to be placed back at the source.
     */
    public List<Coordinate> getLegalDestinations(@Nullable Coordinate square) {
        if (square == null) return Collections.emptyList();
        List<Move> moves = game.getAvailableMoves(square.toString());
        if (moves == null) return Collections.emptyList();

        List<Coordinate> destinations = new ArrayList<>(moves.stream()
                .map(m -> m.getDisplacement().getNewLocation())
                .distinct()
                .toList());
        destinations.add(square);
        return destinations;
    }

    /**
     * Use stored getLegalDestinations of piece instead.
     */
    @Deprecated
    public boolean isMoveLegal(Coordinate source, Coordinate destination) {
        // placing back in original position is also allowed
        if (source.equals(destination)) return true;
        Displacement displacement = createDisplacement(source, destination);
        List<Move> validMoves = findMoves(displacement);
        return !validMoves.isEmpty();
    }

    private boolean equals(Displacement d1, Displacement d2) {
        if (!d1.getOldLocation().equals(d2.getOldLocation())) return false;
        if (!d1.getNewLocation().equals(d2.getNewLocation())) return false;
        if (Piece.fromMoveable(d1.getMoveable()) != Piece.fromMoveable(d2.getMoveable())) return false;

        return true;
    }

    private List<Move> findMoves(Displacement displacement) {
        List<Move> moves = game.getAvailableMoves(displacement.getMoveable());
        if (moves == null) return Collections.emptyList();
        return game.getAvailableMoves(displacement.getMoveable()).stream()
                .filter(m -> equals(m.getDisplacement(), displacement))
                .toList();
    }

    @Nullable
    private Move findMove(Displacement displacement, Piece promotion) {
        Move move = findMoves(displacement).stream().findFirst().orElse(null);
        if (move != null && move.isPromotionNeeded())
            move.setPromotion(promotion.createMoveable());
        return move;
    }

    /**
     * Piece required for promotion
     */
    public boolean doMove(Coordinate source, Coordinate destination, Piece promotion) {
        // placing back in original position is also allowed
        if (source.equals(destination)) {
            clearMinedSquare();
            return true;
        }

        // find intended move
        Displacement displacement = createDisplacement(source, destination);
        Move move = findMove(displacement, promotion);
        if (move == null) {
            ImmersiveChess.LOGGER.error("Could not find valid move for displacement " + displacement.getOldLocation() + " " + displacement.getNewLocation());
            return false;
        }


        // perform the actual move
        try {
            JChessGame.Status status = game.play(move);

            if (move.getLinkedDisplacements() != null)
                move.getLinkedDisplacements().forEach(this::executeDisplacement);

            // en passant happened
            if (move.getCapturedEntity() != game.getPosition().getPreviousPosition().get(move.getDisplacement().getNewLocation())) {
                // perform additional displacement of captured piece to null so it will be removed
                executeDisplacement(new Displacement(move.getCapturedEntity(), game.getPosition().getPreviousPosition().getLocation(move.getCapturedEntity()), null));
            }

            // check if king in check
            Coordinate kingSquare = findKingSquare(getColorOnMove());
            if (world.getBlockEntity(board.getPos(kingSquare)) instanceof BoardBlockEntity boardBlockEntity) {
                boolean kingUnderAttack = game.getPosition().canBeReached(kingSquare, getColorOnMove().opposite());
                boardBlockEntity.setInCheck(kingUnderAttack);
            }

            // check for end of game
            if (status.isFinished()) {
                onGameEnded();
            } else {
                // clear king in check
                Coordinate ownKingSquare = findPreviousKingSquare(getColorOnMove().opposite());
                if (world.getBlockEntity(board.getPos(ownKingSquare)) instanceof BoardBlockEntity boardBlockEntity) {
                    boardBlockEntity.setInCheck(false);
                }
            }

            // set structure again (could have changed while mined)
            updatePieceStructure(move.getDisplacement().getNewLocation());
            clearMinedSquare();

            // clear offer for draw when a move is played
            drawOfferedBy = null;
            markDirty();
            return true;
        } catch (IllegalStateException e) {
            ImmersiveChess.LOGGER.error("Tried to perform move '" + move + "' but failed", e);
            return false;
        }
    }

    private Coordinate findKingSquare(Color color) {
        return findKingSquare(game.getPosition(), color);
    }

    private Coordinate findPreviousKingSquare(Color color) {
        return findKingSquare(game.getPosition().getPreviousPosition(), color);
    }

    private static Coordinate findKingSquare(Position position, Color color) {
        for (Map.Entry<Coordinate, Moveable> entry : position.getMoveables().entrySet()) {
            Moveable moveable = entry.getValue();
            if (moveable.getColor() == color && moveable instanceof King) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No king found on board for color " + color);
    }

    /**
     * Executes move of piece based on displacement. Can be used for complex operations such as en passant and castling or also by AI
     * Note: Moveable piece not taken into account.
     */
    public boolean executeDisplacement(Displacement displacement) {
        BlockPos sourcePos = board.getPos(displacement.getOldLocation()).up();

        // remove origin piece
        breakPiece(displacement.getOldLocation());

        // place destination piece (optional)
        if (displacement.getNewLocation() != null) {
            Piece piece = Piece.fromMoveable(displacement.getMoveable());
            placePiece(displacement.getNewLocation(), piece);
        }

        return true;
    }

    private void placePiece(Coordinate square, Piece piece) {
        BlockPos destPos = board.getPos(square).up();
        Direction facing = piece.getColor() == Color.WHITE ? board.getWhitePlayDirection() : board.getWhitePlayDirection().getOpposite();
        BlockState state = piece.getBlockState(facing);

        if (world.getBlockState(destPos).equals(state))
            return;

        world.setBlockState(destPos, state, Block.NOTIFY_ALL);

        BlockSoundGroup blockSoundGroup = state.getSoundGroup();
        world.playSound(null, destPos, blockSoundGroup.getPlaceSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0f) / 2.0f, blockSoundGroup.getPitch() * 0.8f);

        BlockEntity blockEntity = world.getBlockEntity(destPos);
        if (blockEntity instanceof StructureRenderedBlockEntity pieceBlockEntity) {
            pieceBlockEntity.setStructure(getStructure(piece));
        }
    }

    private void breakPiece(Coordinate square) {
        BlockPos destPos = board.getPos(square).up();
        world.breakBlock(destPos, false);
    }

    public void undoMove() {
        game.back();
        markDirty();
    }

    private static ChessGameState fromNbt(ServerWorld world, NbtCompound nbt) {
        if (!nbt.contains(GAME_KEY))
            return null;

        if (!nbt.contains(BOARD_KEY)) return null;
        MultiblockBoard board = MultiblockBoard.fromNbt(nbt.getCompound(BOARD_KEY));
        if (board == null) return null;

        String pgn = nbt.getString(GAME_KEY);
        try {
            Coordinate minedSquare = nbt.contains(MINED_KEY) ? new Coordinate(nbt.getString(MINED_KEY)) : null;
            Color drawOfferedBy = nbt.contains(DRAW_OFFER_KEY) ? Color.valueOf(nbt.getString(DRAW_OFFER_KEY)) : null;

            // Should only be one game per file
            JChessGame game = new PGNReader(new StringReader(pgn)).readGame();
            ChessGameState state = new ChessGameState(world, game, board, minedSquare, drawOfferedBy);

            if (nbt.contains(WHITE_PLAYER_STRUCTURES_KEY))
                state.playerStructures.put(Color.WHITE, StructureMap.fromNbt(nbt.getCompound(WHITE_PLAYER_STRUCTURES_KEY)));
            if (nbt.contains(BLACK_PLAYER_STRUCTURES_KEY))
                state.playerStructures.put(Color.BLACK, StructureMap.fromNbt(nbt.getCompound(BLACK_PLAYER_STRUCTURES_KEY)));

            if (nbt.contains(WHITE_PIECES_RENDER_OPTION_KEY))
                state.renderOptions.put(Color.WHITE, PieceRenderOption.valueOf(nbt.getString(WHITE_PIECES_RENDER_OPTION_KEY)));
            if (nbt.contains(BLACK_PIECES_RENDER_OPTION_KEY))
                state.renderOptions.put(Color.BLACK, PieceRenderOption.valueOf(nbt.getString(BLACK_PIECES_RENDER_OPTION_KEY)));

            return state;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            ImmersiveChess.LOGGER.error("Failed to load chess game", e);
            return null;
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (drawOfferedBy != null)
            nbt.putString(DRAW_OFFER_KEY, drawOfferedBy.toString());

        nbt.put(BOARD_KEY, board.writeNbt(new NbtCompound()));
        nbt.put(WHITE_PLAYER_STRUCTURES_KEY, getStructuresOfPlayer(Color.WHITE).writeNbt(new NbtCompound()));
        nbt.put(BLACK_PLAYER_STRUCTURES_KEY, getStructuresOfPlayer(Color.BLACK).writeNbt(new NbtCompound()));

        nbt.putString(WHITE_PIECES_RENDER_OPTION_KEY, getRenderOption(Color.WHITE).toString());
        nbt.putString(BLACK_PIECES_RENDER_OPTION_KEY, getRenderOption(Color.BLACK).toString());

        nbt.putString(GAME_KEY, getPgn());
        if (minedSquare != null)
            nbt.putString(MINED_KEY, minedSquare.toString());
        return nbt;
    }

    private String getSavePath() {
        return LOCATION + getGameSaveId();
    }

    public String getGameSaveId() {
        String date = game.getMetadata().getOrDefault(DATE_KEY, "unknown");
        return date + "/" + getGameId();
    }

    private String getPgn() {
        StringWriter strWriter = new StringWriter();
        new PGNWriter(strWriter).writeGame(game);
        return strWriter.toString();
    }

    @Override
    public void save(File file) {
        file.getParentFile().mkdirs();

        // write pgn to file
        File pgnFile = new File(file.getParentFile(), file.getName().replace(".dat", ".pgn"));
        try (FileWriter outputStream = new FileWriter(pgnFile)) {
            outputStream.write(getPgn());
        } catch (IOException e) {
            ImmersiveChess.LOGGER.error("Could not save pgn file", e);
        }

        super.save(file);

        // write nbt or remove data if no longer needed (disabled for now as gameState could still be attached to a board)
//        if (!getStatus().isFinished())
//            super.save(file);
//        else
//            file.delete();
    }
}
