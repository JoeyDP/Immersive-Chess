package be.immersivechess.block.entity;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.structure.StructureHelper;
import be.immersivechess.structure.StructureResolver;
import be.immersivechess.world.MiniatureBlockRenderView;
import be.immersivechess.world.MiniatureWorld;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public abstract class StructureRenderedBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    @Nullable
    private StructureTemplate structure;
    @Nullable
    private MiniatureWorld miniWorld;

    public StructureRenderedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Whether the structure contains at least one light source (can be fully hidden, this is not checked).
     */
    public boolean containsLightSource() {
        if (structure == null) return false;

        return getStructureContent().entrySet().stream().anyMatch(e -> e.getValue().getLuminance() > 0);
    }

    protected Map<BlockPos, BlockState> getStructureContent() {
        return StructureHelper.buildBlockStateMap(structure);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(PieceContainer.NBT_STRUCTURE_KEY)) {
            NbtCompound structureNbt = nbt.getCompound(PieceContainer.NBT_STRUCTURE_KEY);
            setStructureNbt(structureNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // nbt data is passed along to item when mined
        if (structure != null)
            nbt.put(PieceContainer.NBT_STRUCTURE_KEY, getStructureNbt());
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public void setStructureNbt(NbtCompound structureNbt) {
        if (structureNbt == null) {
            setStructure(null);
            return;
        }
        StructureTemplate template = StructureResolver.getStructure(structureNbt);
        setStructure(template);
    }

    public void setStructure(StructureTemplate structure) {
        if (!Objects.equals(this.structure, structure)) {
            this.structure = structure;
            if (this.hasWorld())
                this.miniWorld = new MiniatureWorld(getWorld(), structure);
            markDirty();
            updateBlockModel();
        }
    }

    @Nullable
    public NbtCompound getStructureNbt() {
        if (structure == null) return null;
        return structure.writeNbt(new NbtCompound());
    }

    @Nullable
    public StructureTemplate getStructure() {
        return structure;
    }

    @Nullable
    public MiniatureWorld getMiniWorld(){
        return miniWorld;
    }

    protected void updateBlockModel() {
        if (this.world == null)
            return;

        this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
    }

    @Override
    public @Nullable Object getRenderData() {
        return getStructure();
    }

}
