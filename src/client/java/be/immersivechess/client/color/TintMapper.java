package be.immersivechess.client.color;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a mapping between tintIndices and colorProviders.
 */
public class TintMapper{
    public static final int CAPACITY = 4;

    public static final TintMapper INSTANCE = new TintMapper();

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
    }

    @Nullable
    public BlockColorProvider getBlockColorProvider(int tintIndex){
        int offset = tintIndex - (tintIndex % CAPACITY);
        return BLOCK_PROVIDER_TINT_OFFSETS.inverse().getOrDefault(offset, null);
    }

    public int getTintOffset(@Nullable BlockColorProvider provider){
        if (provider == null)
            return 0;
        return BLOCK_PROVIDER_TINT_OFFSETS.get(provider);
    }

    public int getTintOffset(Block block){
        BlockColorProvider provider = ColorProviderRegistry.BLOCK.get(block);
        return getTintOffset(provider);
    }

}
