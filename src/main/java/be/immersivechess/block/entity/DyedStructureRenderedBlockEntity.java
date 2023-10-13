package be.immersivechess.block.entity;

import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.StandItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public abstract class DyedStructureRenderedBlockEntity extends StructureRenderedBlockEntity{

    private int color = StandItem.DEFAULT_COLOR_INT;

    public DyedStructureRenderedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getColor(){
        return color;
    }

    public void setColor(int color){
        if (this.color != color) {
            this.color = color;
            markDirty();
            updateBlockModel();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains(PieceContainer.NBT_COLOR_KEY))
            color = nbt.getInt(PieceContainer.NBT_COLOR_KEY);
        else
            color = StandItem.DEFAULT_COLOR_INT;

        // to prevent call to updateBlocKModel before all data set
        super.readNbt(nbt);
        updateBlockModel();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // nbt data is passed along to item when mined
        if (color != StandItem.DEFAULT_COLOR_INT)
            nbt.putInt(PieceContainer.NBT_COLOR_KEY, color);
    }

}
