package be.immersivechess.client;

import be.immersivechess.block.Blocks;
import be.immersivechess.block.entity.BlockEntityTypes;
import be.immersivechess.client.color.ColorProviders;
import be.immersivechess.client.gui.ChessCaseScreen;
import be.immersivechess.client.gui.ChessGameScreen;
import be.immersivechess.client.render.block.entity.BoardBlockEntityRenderer;
import be.immersivechess.client.render.block.entity.PieceStandBlockEntityRenderer;
import be.immersivechess.client.render.block.entity.StructureRenderedBlockEntityRenderer;
import be.immersivechess.client.render.block.entity.PieceStructureBlockEntityRenderer;
import be.immersivechess.client.render.model.ModelProvider;
import be.immersivechess.client.structure.ClientStructureResolver;
import be.immersivechess.item.ChessCase;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.screen.ScreenHandlerTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ImmersiveChessClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // ModelProviders
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(rm -> new ModelProvider());

        // Screens
        HandledScreens.register(ScreenHandlerTypes.CHESS_CASE_SCREEN_HANDLER_TYPE, ChessCaseScreen::new);
        HandledScreens.register(ScreenHandlerTypes.CHESS_GAME_SCREEN_HANDLER_TYPE, ChessGameScreen::new);

        // Support transparency in piece models and board blocks. Most of them use material
        // PieceStructureBlock is defined in json and needs cutout for icon overlay
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), Blocks.PIECE_STRUCTURE_BLOCKS.toArray(new Block[0]));
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), Blocks.BOARD_BLOCK);

        // BlockEntityRenderers
        BlockEntityRendererFactories.register(BlockEntityTypes.PIECE_STRUCTURE_BLOCK_ENTITY_TYPE, PieceStructureBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(BlockEntityTypes.BOARD_BLOCK_ENTITY_TYPE, BoardBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(BlockEntityTypes.PIECE_STAND_BLOCK_ENTITY_TYPE, PieceStandBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(BlockEntityTypes.PIECE_BLOCK_ENTITY_TYPE, StructureRenderedBlockEntityRenderer::new);

        // Color providers
        ColorProviders.onInitializeClient();

        // Model predicate for opening chess case when interacted with
        ModelPredicateProviderRegistry.register(Items.CHESS_CASE, new Identifier("open"), (itemStack, clientWorld, livingEntity, i) -> {
            if (livingEntity == null || itemStack == null)
                return 0f;
            if (itemStack.getItem() instanceof ChessCase)
                return ChessCase.isOpen(itemStack) ? 1f : 0f;
            return 0f;
        });

        // Piece stands with default structure
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
            addPieceStands(content);
        });

        ItemGroupEvents.modifyEntriesEvent(Registries.ITEM_GROUP.getKey(Items.IC_GROUP).get()).register(content -> {
            addPieceStands(content);
        });
    }

    private static void addPieceStands(FabricItemGroupEntries content){
        Items.PIECE_STANDS.values().forEach(pieceStandItem -> {
            ItemStack stand = new ItemStack(pieceStandItem);
            StructureTemplate structure = ClientStructureResolver.getDefaultStructure(pieceStandItem.getPiece());
            PieceContainer.writeStructureNbt(stand, structure.writeNbt(new NbtCompound()));
            content.add(stand);
        });
    }

}
