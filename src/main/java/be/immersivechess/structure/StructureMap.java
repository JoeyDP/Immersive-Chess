package be.immersivechess.structure;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.logic.Piece;
import ch.astorm.jchess.core.Color;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StructureMap extends HashMap<Piece, NbtCompound> {
    // Only loaded once
    private static StructureMap DEFAULT_STRUCTURES;

    public StructureMap() {
        super();
    }

    public StructureMap(Map<? extends Piece, ? extends NbtCompound> m) {
        super(m);
    }

    public static StructureMap getDefault(ServerWorld world){
        if (DEFAULT_STRUCTURES == null){
            DEFAULT_STRUCTURES = loadDefault(world);
        }
        return DEFAULT_STRUCTURES;
    }

    public boolean hasAnyOf(Color color){
        for (Piece piece : this.keySet()){
            if (piece.getColor() == color)
                return true;
        }
        return false;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        for (Entry<Piece, NbtCompound> e : this.entrySet()) {
            nbt.put(e.getKey().toString(), e.getValue());
        }
        return nbt;
    }

    @NotNull
    public static StructureMap fromNbt(NbtCompound nbt) {
        StructureMap structures = new StructureMap();

        for (Piece piece : Piece.values()) {
            if (nbt.contains(piece.toString()))
                structures.put(piece, nbt.getCompound(piece.toString()));
        }

        return structures;
    }

    private static StructureMap loadDefault(ServerWorld world) {
        StructureTemplateManager structureTemplateManager = world.getServer().getStructureTemplateManager();
        StructureMap structures = new StructureMap();
        for (Piece piece : Piece.values()) {
            Optional<StructureTemplate> structure = structureTemplateManager.getTemplate(piece.getDefaultStructureIdentifier());
            if (structure.isEmpty()){
                ImmersiveChess.LOGGER.error("Failed to load default structure for piece: " + piece);
                continue;
            }

            structures.put(piece, structure.get().writeNbt(new NbtCompound()));
        }

        return structures;
    }
}
