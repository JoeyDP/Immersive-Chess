package be.immersivechess.resource;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.util.BlockStateUtil;
import ch.astorm.jchess.core.Color;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStateLuminanceMapper extends SinglePreparationResourceReloader<Map<BlockState, Double>> implements IdentifiableResourceReloadListener {
    public static final BlockStateLuminanceMapper INSTANCE = new BlockStateLuminanceMapper();

    // Parsing info
    private static final Identifier BLOCK_COLORS_FILE_ID = new Identifier(ImmersiveChess.MOD_ID, "chess/block_colors.json");
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(BlockState.class, new BlockStateTypeAdapter()).create();

    // definition for Gson
    public static class BlockColorsJson {
        public Map<BlockState, Double> blockstates = new HashMap<>();
    }

    // private attributes
    private Map<BlockState, Double> blockStateLuminances = Collections.emptyMap();
    private double averageLuminance = 50;

    @Nullable
    public Color getColorOfFirstBlock(BlockState state1, BlockState state2) {
        return getLuminance(state1) > getLuminance(state2) ? Color.WHITE : Color.BLACK;
    }

    public double getLuminance(BlockState state) {
        return blockStateLuminances.getOrDefault(state, averageLuminance);
    }

    // For debugging mostly
    public List<BlockState> getOrderedBlockStates(){
        return blockStateLuminances.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(ImmersiveChess.MOD_ID, getName().toLowerCase());
    }

    @Override
    protected Map<BlockState, Double> prepare(ResourceManager manager, Profiler profiler) {
        profiler.startTick();

        Map<BlockState, Double> allData = new HashMap<>();

        // Multiple datapacks might have this resource.
        // By processing all of them we can allow these values to be overridden.
        for (Resource resource : manager.getAllResources(BLOCK_COLORS_FILE_ID)) {
            try (BufferedReader reader = resource.getReader()) {
                BlockColorsJson jsonData = JsonHelper.deserialize(GSON, reader, BlockColorsJson.class);
                if (jsonData.blockstates == null)
                    throw new JsonSyntaxException("Missing attribute 'blockstates' in block colors json");

                allData.putAll(jsonData.blockstates);
            } catch (IOException e) {
                ImmersiveChess.LOGGER.error("Error occurred while loading resource '" + BLOCK_COLORS_FILE_ID + "' from pack '" + resource.getResourcePackName() + "'", e);
            }
        }
        profiler.endTick();
        return allData;
    }

    @Override
    protected void apply(Map<BlockState, Double> prepared, ResourceManager manager, Profiler profiler) {
        profiler.startTick();
        blockStateLuminances = prepared;
        averageLuminance = blockStateLuminances.values().stream().mapToDouble(v -> v).average().orElse(50);
        profiler.endTick();
    }

    // See `BoardColorMapper.GSON` for usage of this outside of the class
    private static class BlockStateTypeAdapter extends TypeAdapter<BlockState> {
        @Override
        public void write(JsonWriter jsonWriter, BlockState blockState) throws IOException {
            jsonWriter.value(blockState.toString());
        }

        @Override
        public BlockState read(JsonReader jsonReader) throws IOException {
            String value = jsonReader.nextString();

            BlockState state = BlockStateUtil.getBlockStateWithName(value);
            if (state == null)
                throw new JsonSyntaxException("Could not find BLockState for " + value);

            return state;
        }
    }

}
