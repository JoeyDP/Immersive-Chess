package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.BoardBlock;
import be.immersivechess.block.PieceBlock;
import be.immersivechess.block.entity.BoardBlockEntity;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.logic.MultiblockBoard;
import be.immersivechess.logic.Piece;
import be.immersivechess.screen.ChessCaseScreenHandler;
import be.immersivechess.structure.StructureMap;
import be.immersivechess.world.ChessGameState;
import ch.astorm.jchess.core.Color;
import ch.astorm.jchess.core.Coordinate;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class ChessCase extends Item {

    private static final String ITEMS_KEY = "Items";
    private static final String OPEN_KEY = "Open";


    public ChessCase(Settings settings) {
        super(settings);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        NamedScreenHandlerFactory namedScreenHandlerFactory = new NamedScreenHandlerFactory() {
            @Override
            public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new ChessCaseScreenHandler(i, playerInventory, ChessCase.getInventory(itemStack), itemStack);
            }

            @Override
            public Text getDisplayName() {
                return itemStack.getName();
            }
        };

        ChessCase.setOpen(itemStack, true);
        player.openHandledScreen(namedScreenHandlerFactory);
        return TypedActionResult.success(itemStack, false);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack chessCase = context.getStack();

        if (player == null)
            return ActionResult.FAIL;

        // when board already exists
        if (world.getBlockState(pos).getBlock() instanceof BoardBlock) {
            // on server
            if (world instanceof ServerWorld serverWorld) {
                if (!(world.getBlockEntity(pos) instanceof BoardBlockEntity boardBlockEntity))
                    return ActionResult.PASS;

                ChessGameState gameState = boardBlockEntity.getGameState();
                if (gameState == null)
                    return ActionResult.PASS;

                // if game finished -> create a new game
                if (gameState.getStatus().isFinished())
                    return initializeBoard(world, player, pos, chessCase);

                // ongoing game (moves start at 1)
                if (gameState.getCurrentMoveIndex() > 1) {
                    player.sendMessage(Text.translatable("immersivechess.game_in_progress"), true);
                    return ActionResult.PASS;
                }

                // initial game (no moves played)
                MultiblockBoard board = gameState.getBoard();
                Color clickedSide = board.getSide(pos);

                StructureMap structures = getStructureNbtForPieces(context.getStack());
                boolean actionPerformed = gameState.togglePlayer(clickedSide, player, structures);
                return actionPerformed ? ActionResult.SUCCESS : ActionResult.FAIL;
            }

            return ActionResult.success(world.isClient());
        }

        // Try to create a board
        return initializeBoard(world, player, pos, chessCase);
    }

    /**
     * Traces board in world, replaces with board blocks if needed, sets new ChessGameState and sets player to be black.
     */
    private ActionResult initializeBoard(World world, PlayerEntity player, BlockPos pos, ItemStack chessCase){
        MultiblockBoard board = MultiblockBoard.getValidBoard(world, player, pos);

        if (board == null) {
            player.sendMessage(Text.translatable("immersivechess.invalid_board"), true);
            return ActionResult.FAIL;
        }

        // on server
        if (world instanceof ServerWorld serverWorld) {
            // create a new chess game
            StructureMap structures = getStructureNbtForPieces(chessCase);
            ChessGameState gameState = ChessGameState.create(serverWorld, board);
            createBoard(world, board, gameState);
            gameState.setPlayer(Color.BLACK, player, structures);
        }

        player.sendMessage(Text.translatable("immersivechess.valid_board"), true);
        return ActionResult.success(world.isClient());
    }


    private static void createBoard(World world, MultiblockBoard board, ChessGameState gameState) {
        // replace with BoardBlock
        BlockBox bounds = board.getBounds();
        for (BlockPos blockPos : BlockPos.iterate(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ())) {
            if (!world.getBlockState(blockPos).isOf(Blocks.BOARD_BLOCK))
                Blocks.BOARD_BLOCK.replace(world, blockPos, board.getColorOfPos(blockPos));

            // clear above
            BoardBlock.breakBlockAboveBoard(world, blockPos.up());

            BlockEntity entity = world.getBlockEntity(blockPos);
            if (entity instanceof BoardBlockEntity boardBlockEntity) {
                Coordinate square = board.getSquare(blockPos);
                boardBlockEntity.setGameState(gameState);
                boardBlockEntity.setSquare(square);
            }
        }

        gameState.placePieces();
    }

    public static boolean isOpen(ItemStack stack) {
        NbtCompound compound = stack.getNbt();
        if (compound != null)
            return compound.getBoolean(OPEN_KEY);

        return false;
    }

    public static void setOpen(ItemStack stack, boolean state) {
        stack.getOrCreateNbt().putBoolean(OPEN_KEY, state);
    }

    @Override
    public boolean canBeNested() {
        // The case can still be placed inside shulkers.
        // Since the case can only contain specific types, it will never cause recursive nesting with other items anyway.
        return true;
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        super.onItemEntityDestroyed(entity);

        ItemUsage.spawnItemContents(entity, getItems(entity.getStack()).stream());
    }

    /**
     * From the chess case item, extract the structure nbt off the piece stands depending on the settings.
     */
    public StructureMap getStructureNbtForPieces(ItemStack stack) {
        Inventory inventory = getInventory(stack);
        return new StructureMap(Arrays.stream(Piece.values())
                .filter(p -> !inventory.getStack(p.ordinal()).isEmpty())
                .collect(Collectors.toMap(
                        p -> p,         // key
                        p -> {          // value
                            ItemStack pieceStack = inventory.getStack(p.ordinal());

                            return PieceContainer.getStructureNbt(pieceStack);
                        })
                ));
    }

    /**
     * Creates an inventory with a listener attached that automatically syncs modifications to the item NBT.
     */
    public static Inventory getInventory(ItemStack stack) {
        SimpleInventory inventory = new SimpleInventory(2 * 6);
        List<ItemStack> items = getItems(stack);
        for (int i = 0; i < items.size(); i++) {
            inventory.setStack(i, items.get(i));
        }

        inventory.addListener(inv -> {
            ChessCase.saveInventory(stack, inv);
        });

        return inventory;
    }

    public static boolean canContainItem(Item item) {
        return item == ItemStack.EMPTY.getItem() || item instanceof PieceStandItem;
    }

    private static List<ItemStack> getItems(ItemStack stack) {
        NbtCompound nbtCompound = stack.getNbt();
        if (nbtCompound == null) {
            return Collections.emptyList();
        }
        NbtList nbtList = nbtCompound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        return nbtList.stream().map(NbtCompound.class::cast).map(ItemStack::fromNbt).toList();
    }

    private static NbtCompound getOrInitializeNbt(ItemStack stack) {
        NbtCompound nbtCompound = stack.getOrCreateNbt();
        if (!nbtCompound.contains(ITEMS_KEY)) {
            nbtCompound.put(ITEMS_KEY, new NbtList());
        }

        NbtList nbtList = nbtCompound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        while (nbtList.size() < 2 * 6) {
            NbtCompound nbtStack = new NbtCompound();
            ItemStack.EMPTY.writeNbt(nbtStack);
            nbtList.add(nbtStack);
        }

        return nbtCompound;
    }

    private static void saveItems(ItemStack caseItemStack, List<ItemStack> items) {
        NbtCompound nbtCompound = getOrInitializeNbt(caseItemStack);

        NbtList nbtList = nbtCompound.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < items.size(); i++) {
            ItemStack itemStack = items.get(i);
            if (!canContainItem(itemStack.getItem())) {
                ImmersiveChess.LOGGER.error("Invalid item encountered in chess case: " + itemStack);
                itemStack = ItemStack.EMPTY;
            }
            NbtCompound nbtStack = new NbtCompound();
            itemStack.writeNbt(nbtStack);
            nbtList.set(i, nbtStack);
        }
    }

    private static void saveInventory(ItemStack stack, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>(inventory.size());
        for (int i = 0; i < inventory.size(); i++) {
            items.add(inventory.getStack(i));
        }
        saveItems(stack, items);
    }

}
