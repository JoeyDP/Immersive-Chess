package be.immersivechess.client.color;

import be.immersivechess.ImmersiveChess;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a mapping between tintIndices and colorProviders.
 */
@Environment(EnvType.CLIENT)
public class TintMapper{
    public static final int CAPACITY = 4;

    // Needs to be defined above the INSTANCE
    public static final BlockColorProvider WATER_COLOR_PROVIDER = (state, world, pos, tintIndex) -> {
        if (world == null) return -1;
        return BiomeColors.getWaterColor(world, pos);
    };

    public static final TintMapper INSTANCE = new TintMapper();

    public final int WATER_COLOR_OFFSET;

    private final BiMap<BlockColorProvider, Integer> BLOCK_PROVIDER_TINT_OFFSETS = HashBiMap.create();

    private TintMapper(){
        // skip first few to accommodate for tints used in the block/item itself
        int offset = CAPACITY;
        for (Block block: Registries.BLOCK){
            BlockColorProvider provider = ColorProviderRegistry.BLOCK.get(block);
            if (BLOCK_PROVIDER_TINT_OFFSETS.containsKey(provider))
                continue;

            BLOCK_PROVIDER_TINT_OFFSETS.put(provider, offset);
            offset += CAPACITY;
        }

        WATER_COLOR_OFFSET = offset;
        BLOCK_PROVIDER_TINT_OFFSETS.put(WATER_COLOR_PROVIDER, WATER_COLOR_OFFSET);
    }

    @Nullable
    public BlockColorProvider getBlockColorProvider(int tintIndex){
        int offset = tintIndex - (tintIndex % CAPACITY);
        return BLOCK_PROVIDER_TINT_OFFSETS.inverse().getOrDefault(offset, null);
    }

    public int getTintOffset(@Nullable BlockColorProvider provider){
        if (provider == null)
            return 0;
        return BLOCK_PROVIDER_TINT_OFFSETS.getOrDefault(provider, -1);
    }

    public int getTintOffset(Block block){
        BlockColorProvider provider = ColorProviderRegistry.BLOCK.get(block);
        return getTintOffset(provider);
    }

}
