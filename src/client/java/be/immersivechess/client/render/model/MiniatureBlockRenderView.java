package be.immersivechess.client.render.model;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;

public class MiniatureBlockRenderView implements BlockRenderView, LightSourceView {

    private final Map<BlockPos, BlockState> blockStates;

    private final ChunkSkyLight chunkSkyLight;
    private final LightingProvider lightingProvider;

    public MiniatureBlockRenderView(Map<BlockPos, BlockState> blockStates) {
        this.blockStates = blockStates;
        chunkSkyLight = new ChunkSkyLight(this);
        lightingProvider = createLightingProvider();
    }

    private LightingProvider createLightingProvider() {
        LightSourceView view = this;
        ChunkProvider chunkProvider = new ChunkProvider() {
            @Nullable
            @Override
            public LightSourceView getChunk(int chunkX, int chunkZ) {
                if (chunkX == 0 && chunkZ == 0)
                    return view;
                return null;
            }

            @Override
            public BlockView getWorld() {
                return view;
            }
        };

        // always use skylight, because this would have to depend on where the piece is built and we don't store this info.
        LightingProvider p = new LightingProvider(chunkProvider, true, true);

        // enable light updates for the chunk
        p.setSectionStatus(new BlockPos(0, 0, 0), false);
        p.propagateLight(new ChunkPos(0, 0));
        p.doLightUpdates();

        return p;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        // Brightness gets applied again when rendering to world, and we don't want it twice
        return 1;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return lightingProvider;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        // Ideally, we don't want to color the cached model yet, but have it get its color when placed.
        return ColorHelper.Argb.getArgb(255, 255, 255, 255);
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public int getBottomY() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        // TODO
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blockStates.getOrDefault(pos, Blocks.VOID_AIR.getDefaultState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.getDefaultState();
    }

    @Override
    public void forEachLightSource(BiConsumer<BlockPos, BlockState> callback) {
        blockStates.forEach((pos, bs) -> {
            if (bs.getLuminance() > 0) callback.accept(pos, bs);
        });
    }

    @Override
    public ChunkSkyLight getChunkSkyLight() {
        return chunkSkyLight;
    }
}