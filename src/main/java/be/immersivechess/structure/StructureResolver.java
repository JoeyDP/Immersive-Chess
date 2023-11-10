package be.immersivechess.client.structure;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.logic.Piece;
import com.google.common.collect.MapMaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class StructureResolver {

    private static final Map<NbtCompound, StructureTemplate> cache = new MapMaker().weakValues().makeMap();

    private static final StructureTemplateManager structureTemplateManager = getStructureTemplateManager();

    private static StructureTemplateManager getStructureTemplateManager(){
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.isIntegratedServerRunning())
            return client.getServer().getStructureTemplateManager();

        try {
            // session only used to store structures -> won't be used
            LevelStorage.Session session = client.getLevelStorage().createSession("");
            session.close();
            LifecycledResourceManagerImpl dataResourceManager = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, client.getResourcePackManager().createResourcePacks());
            return new StructureTemplateManager(dataResourceManager, session, client.getDataFixer(), Registries.BLOCK.getReadOnlyWrapper());
        } catch (IOException | SymlinkValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private static StructureTemplate loadStructure(NbtCompound compound){
        return structureTemplateManager.createTemplate(compound);
    }

    public static StructureTemplate getStructure(NbtCompound compound){
        return cache.computeIfAbsent(compound, StructureResolver::loadStructure);
    }

    public static StructureTemplate getDefaultStructure(Piece piece){
        Optional<StructureTemplate> structureTemplateOptional = structureTemplateManager.getTemplate(piece.getDefaultStructureIdentifier());
        return structureTemplateOptional.orElse(null);
    }

    @Nullable
    public static StructureTemplate getStructure(ItemStack stack){
        if (!(stack.getItem() instanceof PieceContainer pieceContainer))
            return null;

        if (pieceContainer.getPiece() == null)
            return null;

        NbtCompound structureNbt = PieceContainer.getStructureNbt(stack);
        if (structureNbt.isEmpty())
            return getDefaultStructure(pieceContainer.getPiece());

        return getStructure(structureNbt);
    }

}
