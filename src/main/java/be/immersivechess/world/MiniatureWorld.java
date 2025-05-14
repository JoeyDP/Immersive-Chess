package be.immersivechess.world;

import be.immersivechess.structure.StructureHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.QueryableTickScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MiniatureWorld extends World implements LightSourceView {

    private final Map<BlockPos, BlockState> blockStates;
    private final Map<BlockPos, BlockEntity> blockEntities;

    private final ChunkSkyLight chunkSkyLight;
    private final LightingProvider lightingProvider;

    public MiniatureWorld(World globalWorld, StructureTemplate structure) {
        this(globalWorld, StructureHelper.buildBlockStateMap(structure), StructureHelper.buildBlockEntityMap(structure));
    }

    public MiniatureWorld(World globalWorld, Map<BlockPos, BlockState> blockStates, Map<BlockPos, BlockEntity> blockEntities) {
        super((MutableWorldProperties) globalWorld.getLevelProperties(), globalWorld.getRegistryKey(),
                globalWorld.getRegistryManager(), globalWorld.getDimensionEntry(),
                globalWorld.getProfilerSupplier(),
                globalWorld.isClient, globalWorld.isDebugWorld(), 0, 16);
        this.blockStates = blockStates;
        this.blockEntities = blockEntities;

        this.blockEntities.forEach((pos, be) -> be.setWorld(this));

        chunkSkyLight = new ChunkSkyLight(this);
        lightingProvider = createLightingProvider();
    }

    public Map<BlockPos, BlockState> getBlockStates() {
        return blockStates;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return blockEntities;
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
    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

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
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return null;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return null;
    }

    @Override
    public int getBottomY() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Nullable
    @Override
    public Entity getEntityById(int id) {
        return null;
    }

    @Nullable
    @Override
    public MapState getMapState(String id) {
        return null;
    }

    @Override
    public void putMapState(String id, MapState state) {

    }

    @Override
    public int getNextMapId() {
        return 0;
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {

    }

    @Override
    public Scoreboard getScoreboard() {
        return null;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return null;
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup() {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blockStates.getOrDefault(pos, Blocks.VOID_AIR.getDefaultState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public void playSound(@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public String asString() {
        return null;
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

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        return null;
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        return null;
    }

    @Override
    public ChunkManager getChunkManager() {
        return null;
    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return null;
    }
}
