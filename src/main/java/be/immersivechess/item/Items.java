package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.PieceBlock;
import be.immersivechess.block.PieceStandBlock;
import be.immersivechess.block.PieceStructureBlock;
import be.immersivechess.logic.Piece;
import be.immersivechess.recipe.StandDyeRecipe;
import be.immersivechess.resource.BlockStateLuminanceMapper;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Items {

//    public static final Item DEBUG_ITEM = register("debug", new Item(new FabricItemSettings()) {
//        @Override
//        public ActionResult useOnBlock(ItemUsageContext context) {
//            context = new ItemPlacementContext(context);
//            BlockPos pos = context.getBlockPos();
//            Direction dir = context.getHorizontalPlayerFacing();
//
//            boolean noteblockDone = false;
//            for (BlockState state : BlockStateLuminanceMapper.INSTANCE.getOrderedBlockStates()) {
//                if (state.isOf(net.minecraft.block.Blocks.NOTE_BLOCK)) {
//                    if (noteblockDone)
//                        continue;
//                    noteblockDone = true;
//                }
//                context.getWorld().setBlockState(pos, state);
//                pos = pos.offset(dir);
//            }
//
//            return ActionResult.success(context.getWorld().isClient);
//        }
//    });

    // Items
    public static final Item CHESS_CASE = register("chess_case", new ChessCase(new FabricItemSettings().maxCount(1)));

    public static final Item TEST_ITEM = register("test_item", new TestItem(Blocks.BLANK_STAND, new FabricItemSettings().maxCount(64)));

    // BlockItems
    // Stands
    public static final Item BLANK_STAND = register(new StandItem(Blocks.BLANK_STAND, new FabricItemSettings()));
    public static final SortedMap<Piece, PieceStandItem> PIECE_STANDS = Blocks.PIECE_STANDS.stream().collect(Collectors.toMap(
            PieceStandBlock::getPiece,
            Items::createPieceStand,
            (v1, v2) -> {
                throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
            },
            TreeMap::new)
    );

    // Pieces
    public static final SortedMap<Piece, Item> PIECE_ITEMS = Blocks.PIECES.values().stream().collect(Collectors.toMap(
            PieceBlock::getPiece,
            Items::createPieceBlockItems,
            (v1, v2) -> {
                throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
            },
            TreeMap::new)
    );

    public static final List<Item> PIECE_STRUCTURE_ITEMS = Blocks.PIECE_STRUCTURE_BLOCKS.stream().map(Items::createPieceStructureBlockItems).toList();

    private static PieceItem createPieceBlockItems(PieceBlock pieceBlock) {
        return register(new PieceItem(pieceBlock, new FabricItemSettings().maxCount(1)));
    }

    private static Item createPieceStructureBlockItems(PieceStructureBlock pieceStructureBlock) {
        return register(pieceStructureBlock);
    }

    private static PieceStandItem createPieceStand(PieceStandBlock pieceStandBlock) {
        return register(new PieceStandItem(pieceStandBlock, new FabricItemSettings().maxCount(1)));
    }

    private static BlockItem register(Block block) {
        return Items.register(new BlockItem(block, new FabricItemSettings()));
    }

    private static <T extends BlockItem> T register(T item) {
        return Items.register(item.getBlock(), item);
    }

    protected static <T extends Item> T register(Block block, T item) {
        return Items.register(Registries.BLOCK.getId(block), item);
    }

    private static <T extends Item> T register(String id, T item) {
        return Items.register(new Identifier(ImmersiveChess.MOD_ID, id), item);
    }

    private static <T extends Item> T register(Identifier id, T item) {
        return Registry.register(Registries.ITEM, id, item);
    }

    // Item Groups
    public static final ItemGroup IC_GROUP = register(
            new Identifier(ImmersiveChess.MOD_ID, "immersivechess"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(CHESS_CASE))
    );

    private static ItemGroup register(Identifier id, ItemGroup.Builder builder) {
        ItemGroup group = builder
                .displayName(Text.translatable(id.toTranslationKey("itemGroup")))
                .build();
        return Registry.register(Registries.ITEM_GROUP, id, group);
    }

    public static void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(CHESS_CASE);

            // debug items
//            content.add(DEBUG_ITEM);
            content.add(TEST_ITEM);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
            addStandsToItemGroup(content);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            PIECE_STRUCTURE_ITEMS.forEach(content::add);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COLORED_BLOCKS).register(content -> {

        });

        // also add everything to immersivechess tab
        ItemGroupEvents.modifyEntriesEvent(Registries.ITEM_GROUP.getKey(IC_GROUP).get()).register(content -> {
            content.add(CHESS_CASE);
            PIECE_STRUCTURE_ITEMS.forEach(content::add);
            addStandsToItemGroup(content);
        });
    }

    private static void addStandsToItemGroup(FabricItemGroupEntries content) {
        content.add(BLANK_STAND);

        // stands in all colors
        for (DyeColor dyeColor : DyeColor.values()) {
            // yellow is added as undyed beacuse it is the default
            if (dyeColor.equals(StandItem.DEFAULT_COLOR))
                continue;

            ItemStack stand = new ItemStack(BLANK_STAND);
            PieceContainer.writeColor(stand, StandDyeRecipe.dyeColorToColor(dyeColor));
            content.add(stand);
        }

        // Piece stands with default structures are registered on the client
    }

}
