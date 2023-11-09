package be.immersivechess.block.entity;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.mixin.MixinStructureAccessor;
import be.immersivechess.structure.StructureHelper;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.StructureAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class StructureRenderedBlockEntity extends BlockEntity implements RenderAttachmentBlockEntity {
    private NbtCompound structureNbt;

    public StructureRenderedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Whether the structure contains at least one light source (can be fully hidden, this is not checked).
     */
    public boolean containsLightSource(){
        if (structureNbt == null) return false;

        StructureTemplate template = new StructureTemplate();
        template.readNbt(Registries.BLOCK.getReadOnlyWrapper(), structureNbt);

        return StructureHelper.buildBlockStateMap(template).entrySet().stream().anyMatch(e -> e.getValue().getLuminance() > 0);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(PieceContainer.NBT_STRUCTURE_KEY)){
            structureNbt = nbt.getCompound(PieceContainer.NBT_STRUCTURE_KEY);
            updateBlockModel();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // nbt data is passed along to item when mined
        if (structureNbt != null)
            nbt.put(PieceContainer.NBT_STRUCTURE_KEY, structureNbt);
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

    public void setStructure(NbtCompound structureNbt) {
        if (!Objects.equals(this.structureNbt, structureNbt)){
            this.structureNbt = structureNbt;
            markDirty();
            updateBlockModel();
        }
    }

    public void setStructure(StructureTemplate structure) {
        setStructure(structure.writeNbt(new NbtCompound()));
    }

    @Nullable
    public NbtCompound getStructureNbt() {
        return structureNbt;
    }

    protected void updateBlockModel() {
        if (this.world == null)
            return;

        this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
    }

    @Override
    public @Nullable Object getRenderAttachmentData() {
        return getStructureNbt();
    }

}
