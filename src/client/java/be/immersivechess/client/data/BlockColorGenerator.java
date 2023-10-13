package be.immersivechess.client.data;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.client.mixin.SpriteContentsAccess;
import be.immersivechess.logic.MultiblockBoard;
import be.immersivechess.resource.BlockStateLuminanceMapper;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.WindowProvider;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.Registries;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

class BlockColorGenerator implements DataProvider {

    private final DataOutput.PathResolver pathResolver;

    public BlockColorGenerator(FabricDataOutput dataOutput) {
        this.pathResolver = dataOutput.getResolver(DataOutput.OutputType.DATA_PACK, "chess");
    }

    @Override
    public String getName() {
        return "Chess Block Color";
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        BlockStateLuminanceMapper.BlockColorsJson data = getJsonBlockStates();

        JsonElement root = BlockStateLuminanceMapper.GSON.toJsonTree(data);

        return DataProvider.writeToPath(writer, root, pathResolver.resolveJson(new Identifier(ImmersiveChess.MOD_ID, "block_colors")));
    }

    private BlockStateLuminanceMapper.BlockColorsJson getJsonBlockStates() {
        BlockStateLuminanceMapper.BlockColorsJson data = new BlockStateLuminanceMapper.BlockColorsJson();
        BlockModels blockModels = loadBlockModels();

        for (Block block : Registries.BLOCK) {
            for (BlockState state : block.getStateManager().getStates()) {
                if (!MultiblockBoard.isBoardMaterial(state))
                    continue;
                OptionalDouble color = getPerceivedLuminanceOfBlockStateTop(state, blockModels);
                if (color.isEmpty()) {
                    ImmersiveChess.LOGGER.info("No color computed for " + state);
                    continue;
                }
                data.blockstates.put(state, color.getAsDouble());
            }
        }

        return data;
    }

    private static OptionalDouble getPerceivedLuminanceOfBlockStateTop(BlockState state, BlockModels blockModels) {
        Random random = Random.create(42);

        BakedModel model = blockModels.getModel(state);
        if (model == null)
            return OptionalDouble.empty();

        List<BakedQuad> quads = model.getQuads(state, Direction.UP, random);
        return quads.stream().mapToDouble(BlockColorGenerator::averagePerceivedLuminance).average();
    }

    private static double averagePerceivedLuminance(BakedQuad quad) {
        // Color index depends on position in world.
        //  could take average of possible biome colors and use image.blend (see `BiomeColors`). Would only matter for grass
        //  or just ignore and continue with uncolored version which likely gives a good estimate.
//            quad.getColorIndex();

        SpriteContents contents = quad.getSprite().getContents();
        NativeImage image = ((SpriteContentsAccess) contents).getImage();
        return ImageHelper.averagePerceivedLuminance(image);
    }

    private BlockModels loadBlockModels() {
        // Datagen is injected before models are loaded. The following code is copied from MinecraftClient to
        // load the models
        MinecraftClient client = MinecraftClient.getInstance();

        RenderSystem.initBackendSystem();
        WindowProvider windowProvider = new WindowProvider(client);
        WindowSettings windowSettings = new WindowSettings(1, 1, OptionalInt.of(1), OptionalInt.of(1), false);
        Window window = windowProvider.createWindow(windowSettings, null, "");

        ReloadableResourceManagerImpl resourceManager = new ReloadableResourceManagerImpl(ResourceType.CLIENT_RESOURCES);
        TextureManager textureManager = new TextureManager(resourceManager);
        // somehow the reloader gets stuck on this one
//            resourceManager.registerReloader(textureManager);

        // probably optional
//            resourceManager.registerReloader(new GrassColormapResourceSupplier());
//            resourceManager.registerReloader(new FoliageColormapResourceSupplier());

        BlockColors blockColors = BlockColors.create();
        BakedModelManager bakedModelManager = new BakedModelManager(textureManager, blockColors, 0);
        resourceManager.registerReloader(bakedModelManager);

        ResourcePackManager resourcePackManager = client.getResourcePackManager();
        resourcePackManager.scanPacks();
        List<ResourcePack> list = resourcePackManager.createResourcePacks();
        CompletableFuture<Unit> COMPLETED_UNIT_FUTURE = CompletableFuture.completedFuture(Unit.INSTANCE);

        // Use client executor rather than `Util.getMainWorkerExecutor()` (as in MinecraftClient) because it doesn't work
        ResourceReload resourceReload = resourceManager.reload(client, client, COMPLETED_UNIT_FUTURE, list);
        resourceReload.whenComplete().join();

        return bakedModelManager.getBlockModels();
    }

}
