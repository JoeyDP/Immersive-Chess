package be.immersivechess.util;

import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class BlockStateUtil {

    @Nullable
    public static BlockState getBlockStateWithName(String name) {
        if (!name.startsWith("Block{"))
            throw new JsonSyntaxException("Invalid BlockState identifier: " + name);

        String blockId = name.substring(6, name.indexOf('}'));
        Block block = Registries.BLOCK.get(new Identifier(blockId));

        return getBlockStateWithName(block, name);
    }

    @Nullable
    private static BlockState getBlockStateWithName(Block block, String name) {
        for (BlockState state : block.getStateManager().getStates()) {
            if (state.toString().equals(name))
                return state;
        }
        return null;
    }

}
