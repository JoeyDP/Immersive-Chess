package be.immersivechess.logic;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.BoardBlock;
import be.immersivechess.resource.BlockStateLuminanceMapper;
import ch.astorm.jchess.core.Color;
import ch.astorm.jchess.core.Coordinate;
import com.google.common.collect.Iterables;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MultiblockBoard {
    // constants
    public static final int BOARD_SIZE = 8;

    // nbt keys
    private static final String A1_KEY = "A1";
    private static final String WHITE_DIRECTION_KEY = "WhiteDirection";

    // properties
    // Position corresponding to A1 square (black) which is used to map all squares together with play direction.
    private final BlockPos a1;
    // Direction in the world towards which white is playing.
    private final Direction whitePlayDirection;
    private final BlockBox bounds;

    private MultiblockBoard(BlockPos a1, Direction whiteDirection) {
        this(a1, whiteDirection, computeBounds(a1, whiteDirection));
    }

    private MultiblockBoard(BlockPos a1, Direction whiteDirection, BlockBox bounds) {
        this.a1 = a1;
        this.whitePlayDirection = whiteDirection;
        this.bounds = bounds;
    }

    private static BlockBox computeBounds(BlockPos a1, Direction whiteDirection) {
        return BlockBox.create(a1, a1.offset(whiteDirection, BOARD_SIZE - 1).offset(whiteDirection.rotateYClockwise(), BOARD_SIZE - 1));
    }

    public BlockBox getBounds() {
        return bounds;
    }

    public Direction getWhitePlayDirection() {
        return whitePlayDirection;
    }

    @Nullable
    public Coordinate getSquare(BlockPos pos) {
        if (!bounds.contains(pos))
            return null;

        BlockPos localPos = pos.mutableCopy().subtract(a1);

        Direction.Axis forwardAxis = whitePlayDirection.getAxis();
        Direction.Axis sidewaysAxis = forwardAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        int forward = Math.abs(localPos.getComponentAlongAxis(forwardAxis));
        int sideways = Math.abs(localPos.getComponentAlongAxis(sidewaysAxis));

        return new Coordinate(forward, sideways);
    }

    public BlockPos getPos(Coordinate coordinate) {
        return a1.offset(whitePlayDirection, coordinate.getRow()).offset(whitePlayDirection.rotateYClockwise(), coordinate.getColumn());
    }

    /**
     * Returns the color of the square
     */
    @Nullable
    public Color getColorOfPos(BlockPos pos) {
        if (!bounds.contains(pos)) return null;
        BlockPos diff = pos.subtract(a1);
        int index = Math.abs(diff.getX()) + Math.abs(diff.getZ());
        return index % 2 == 0 ? Color.BLACK : Color.WHITE;
    }

    public Color getSide(BlockPos pos) {
        return getSide(getSquare(pos));
    }

    public Color getSide(Coordinate square) {
        if (square == null) return null;
        return square.getRow() <= 3 ? Color.WHITE : Color.BLACK;
    }

    /**
     * Returns the bounds of a valid board including position or null if not valid;
     */
    @Nullable
    public static MultiblockBoard getValidBoard(World world, PlayerEntity player, BlockPos origin) {
        BlockState material1 = getAppearanceState(world, origin);

        if (!isBoardMaterial(material1))
            return null;

        double material1Luminance = BlockStateLuminanceMapper.INSTANCE.getLuminance(material1);

        // Create list of candidate materials in the four cardinal directions around origin.
        List<BlockState> candidateMaterials = Stream.of(
                        getAppearanceState(world, origin.offset(Direction.NORTH)),
                        getAppearanceState(world, origin.offset(Direction.EAST)),
                        getAppearanceState(world, origin.offset(Direction.SOUTH)),
                        getAppearanceState(world, origin.offset(Direction.WEST))
                )
                // Count occurences
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                .entrySet().stream()
                // Filter materials that occur at least twice and are valid and different from first material
                .filter(entry -> entry.getValue() > 1 && isBoardMaterial(entry.getKey()) && entry.getKey() != material1)
                // Don't allow luminance to be the same because then we can't deterministically set black and white colors
                .filter(entry -> BlockStateLuminanceMapper.INSTANCE.getLuminance(entry.getKey()) != material1Luminance)
                .map(Map.Entry::getKey).toList();

        // Check for three directions. If valid, at least one of those will be correct.
        for (BlockState material2 : candidateMaterials) {
            ImmersiveChess.LOGGER.debug("Trying to validate board with materials " + material1 + " and " + material2);
            MultiblockBoard board;
            if ((board = getValidBoard(world, player, origin, material1, material2)) != null) {
                return board;
            }
        }

        return null;
    }

    /**
     * Check if valid board can be formed at position with given materials.
     */
    @Nullable
    public static MultiblockBoard getValidBoard(World world, PlayerEntity player, BlockPos origin, BlockState material1, BlockState material2) {
        // Check materials
        if (!isBoardMaterial(material1) || !isBoardMaterial(material2))
            return null;

        BlockState[] materials = {material1, material2};

        BlockBox bounds = traceBoardBounds(world, origin, materials);

        if (bounds.getBlockCountX() != BOARD_SIZE) {
            ImmersiveChess.LOGGER.debug("Invalid board size in X axis: " + bounds.getBlockCountX());
            return null;
        }

        if (bounds.getBlockCountZ() != BOARD_SIZE) {
            ImmersiveChess.LOGGER.debug("Invalid board size in Z axis: " + bounds.getBlockCountZ());
            return null;
        }

        // Check material in board and edges
        return getValidBoard(world, player, origin, materials, bounds);
    }

    /**
     * Check if board with bounds can be formed with given materials.
     * <p>
     * Also checks if the border is made of a different material.
     */
    @Nullable
    public static MultiblockBoard getValidBoard(World world, PlayerEntity player, BlockPos origin, BlockState[] materials, BlockBox bounds) {
        assert materials.length == 2;
        assert bounds.getBlockCountY() == 1;

        if (!checkBoardMaterials(world, origin, materials, bounds)) return null;
        if (!checkBoardBorder(world, origin, materials, bounds)) return null;
        if (!checkBoardAccessible(world, player, bounds)) return null;

        // determine white and black material based on luminance
        Color c = BlockStateLuminanceMapper.INSTANCE.getColorOfFirstBlock(materials[0], materials[1]);
        BlockState blackBlock = c == Color.BLACK ? materials[0] : materials[1];
        BlockState whiteBlock = c == Color.BLACK ? materials[1] : materials[0];

        // find a1 based on player that selected origin being black
        // first find two black corners
        BlockPos center = bounds.getCenter();
        List<BlockPos> blackCorners = BlockPos.stream(bounds)
                .map(BlockPos::new)     // copy pos, because the same variable gets set in the iterator
                .filter(p -> getAppearanceState(world, p).equals(blackBlock))
                .filter(p -> p.getSquaredDistanceFromCenter(center.getX(), center.getY(), center.getZ()) >= 2 * Math.pow(3.5, 2) - 0.01f)
                .toList();

        assert blackCorners.size() == 2;
        BlockPos c1 = blackCorners.get(0);
        BlockPos c2 = blackCorners.get(1);

        // find forward/backward direction to determine side
        // assume c1 as corner on white side for computation, gets swapped if incorrect
        BlockPos facing = c2.subtract(c1);

        Direction d1 = Direction.getFacing(facing.getX(), 0, 0);
        Direction d2 = Direction.getFacing(0, 0, facing.getZ());
        Direction forward = d1.rotateYClockwise().equals(d2) ? d1 : d2;

        // side closest to click position (origin) is black
        int dist1 = Math.abs(c1.subtract(origin).getComponentAlongAxis(forward.getAxis()));
        int dist2 = Math.abs(c2.subtract(origin).getComponentAlongAxis(forward.getAxis()));
        if (dist1 < dist2) {
            c1 = c2;
            forward = forward.getOpposite();
        }

        return new MultiblockBoard(c1, forward, bounds);
    }

    public void endBoardBlocks(World world) {
        for (BlockPos blockPos : BlockPos.iterate(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ())) {
            BoardBlock.placeBack(world, blockPos);
        }
    }

    /**
     * Check if board of right materials.
     */
    private static boolean checkBoardMaterials(World world, BlockPos origin, BlockState[] materials, BlockBox bounds) {
        for (BlockPos pos : (Iterable<BlockPos>) BlockPos.stream(bounds)::iterator) {
            int distance = pos.getManhattanDistance(origin);
            if (!getAppearanceState(world, pos).equals(materials[distance % 2])) {
                ImmersiveChess.LOGGER.debug("Chess board material invalid at " + pos);
                ImmersiveChess.LOGGER.debug("Expected " + materials[distance % 2] + " but got " + getAppearanceState(world, pos));
                return false;
            }
        }
        return true;
    }

    /**
     * Check if borders are of different material than main board.
     */
    private static boolean checkBoardBorder(World world, BlockPos origin, BlockState[] materials, BlockBox bounds) {
        assert bounds.getBlockCountY() == 1;

        Iterable<BlockPos> borderBlocks = BlockPos.iterate(
                bounds.getMinX() - 1, bounds.getMinY(), bounds.getMinZ() - 1,
                bounds.getMaxX() + 1, bounds.getMinY(), bounds.getMinZ() - 1);
        borderBlocks = Iterables.concat(borderBlocks, BlockPos.iterate(
                bounds.getMinX() - 1, bounds.getMinY(), bounds.getMaxZ() + 1,
                bounds.getMaxX() + 1, bounds.getMinY(), bounds.getMaxZ() + 1));
        borderBlocks = Iterables.concat(borderBlocks, BlockPos.iterate(
                bounds.getMaxX() + 1, bounds.getMinY(), bounds.getMinZ(), // don't need +1 because already checked
                bounds.getMaxX() + 1, bounds.getMinY(), bounds.getMaxZ()));
        borderBlocks = Iterables.concat(borderBlocks, BlockPos.iterate(
                bounds.getMinX() - 1, bounds.getMinY(), bounds.getMinZ(),
                bounds.getMinX() - 1, bounds.getMinY(), bounds.getMaxZ()));

        for (BlockPos pos : borderBlocks) {
            BlockState bs = getAppearanceState(world, pos);
            if (bs.equals(materials[0]) || bs.equals(materials[1])) {
                ImmersiveChess.LOGGER.debug("Invalid block " + bs + " at position " + pos);
                ImmersiveChess.LOGGER.debug("Borders of chess board are not allowed to be of same material");
                return false;
            }
            BlockState appearanceState = bs.getAppearance(world, pos, Direction.UP, null, null);
            if (appearanceState.equals(materials[0]) || appearanceState.equals(materials[1])) {
                ImmersiveChess.LOGGER.debug("Invalid block " + bs + " at position " + pos);
                ImmersiveChess.LOGGER.debug("Borders of chess board are not allowed to appear as the same material");
                return false;
            }
            if (bs.isOf(be.immersivechess.block.Blocks.BOARD_BLOCK)) {
                ImmersiveChess.LOGGER.debug("Invalid block " + bs + " at position " + pos);
                ImmersiveChess.LOGGER.debug("Multiple chess boards are not allowed next to each other");
                return false;
            }
        }

        return true;
    }

    private static boolean checkBoardAccessible(World world, PlayerEntity player, BlockBox bounds) {
        for (BlockPos pos : (Iterable<BlockPos>) BlockPos.stream(bounds)::iterator) {
            if (!world.canPlayerModifyAt(player, pos)) return false;
        }
        return true;
    }

    /**
     * Traces the map from a given origin to see how far the board could go.
     * Checks up until sizes of 2 * BOARD_SIZE.
     */
    private static BlockBox traceBoardBounds(World world, BlockPos origin, BlockState[] materials) {
        // Determine bounds of board by tracing X and Y axis from origin piece
        Pair<Integer, Integer> boundsX = getValidLength(world, origin, materials, Direction.Axis.X);
        Pair<Integer, Integer> boundsZ = getValidLength(world, origin, materials, Direction.Axis.Z);
        // Form bounding box
        return BlockBox.create(
                new Vec3i(boundsX.getLeft(), origin.getY(), boundsZ.getLeft()),
                new Vec3i(boundsX.getRight(), origin.getY(), boundsZ.getRight())
        );
    }

    /**
     * Determine bounds of board by tracing axis from origin piece
     */
    private static Pair<Integer, Integer> getValidLength(World world, BlockPos origin, BlockState[] materials, Direction.Axis axis) {
        assert materials.length == 2;

        Pair<Integer, Integer> result = new Pair<>(null, null);
        for (int sign = -1; sign < 2; sign += 2) {
            int limit = origin.getComponentAlongAxis(axis);
            for (int step = 1; step < BOARD_SIZE; step++) {
                BlockPos pos = origin.offset(axis, sign * step);
                int distance = pos.getManhattanDistance(origin);
                if (!getAppearanceState(world, pos).equals(materials[distance % 2])) break;
                limit = pos.getComponentAlongAxis(axis);
            }
            if (result.getLeft() == null) {
                result.setLeft(limit);
            } else {
                result.setRight(limit);
            }

        }
        return result;
    }

    private static BlockState getAppearanceState(BlockRenderView world, BlockPos pos){
        return world.getBlockState(pos).getAppearance(world, pos, Direction.UP, null, null);
    }

    public static boolean isBoardMaterial(BlockState blockState) {
        if (blockState.getBlock() instanceof BoardBlock) return true;
        if (blockState.equals(Blocks.AIR.getDefaultState())) return false;
        if (!blockState.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) return false;
        if (blockState.hasBlockEntity()) return false;
        return true;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put(A1_KEY, NbtHelper.fromBlockPos(a1));
        nbt.put(WHITE_DIRECTION_KEY, NbtHelper.fromBlockPos(new BlockPos(whitePlayDirection.getVector())));
        return nbt;
    }

    public static MultiblockBoard fromNbt(NbtCompound nbt) {
        if (!nbt.contains(A1_KEY) || !nbt.contains(WHITE_DIRECTION_KEY)) return null;

        BlockPos a1 = NbtHelper.toBlockPos(nbt.getCompound(A1_KEY));
        Direction whitePlayDirection = Direction.fromVector(NbtHelper.toBlockPos(nbt.getCompound(WHITE_DIRECTION_KEY)));

        return new MultiblockBoard(a1, whitePlayDirection);
    }
}
