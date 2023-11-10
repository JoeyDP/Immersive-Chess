package be.immersivechess.structure;

import com.google.common.collect.MapMaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class StructureResolver {

    private static final Map<NbtCompound, StructureTemplate> cache = new MapMaker().weakValues().makeMap();

    public static StructureTemplate getStructure(NbtCompound compound){
        return cache.computeIfAbsent(compound, StructureResolver::loadStructure);
    }

    private static StructureTemplate loadStructure(NbtCompound structureNbt){
        StructureTemplate structure = new StructureTemplate();
        int version = NbtHelper.getDataVersion(structureNbt, 500);
        structureNbt = DataFixTypes.STRUCTURE.update(Schemas.getFixer(), structureNbt, version);
        structure.readNbt(Registries.BLOCK.getReadOnlyWrapper(), structureNbt);

        return structure;
    }
}
