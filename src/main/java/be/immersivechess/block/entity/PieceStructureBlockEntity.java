package be.immersivechess.block.entity;

import be.immersivechess.block.PieceStructureBlock;
import be.immersivechess.item.Items;
import be.immersivechess.item.PieceContainer;
import be.immersivechess.item.StandItem;
import be.immersivechess.logic.Piece;
import be.immersivechess.structure.StructureHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PieceStructureBlockEntity extends DyedStructureRenderedBlockEntity implements GameEventListener {
    // keys for nbt data
    private static final String AUTHOR_KEY = "Author";

    // cached properties
    private final BlockBox buildingBox;
    private final PositionSource listenerSource;

    // stored to nbt
    private String authorName;

    public PieceStructureBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.PIECE_STRUCTURE_BLOCK_ENTITY_TYPE, pos, state);

        buildingBox = ((PieceStructureBlock) state.getBlock()).getBuildingBox(state, pos);
        listenerSource = new BlockPositionSource(buildingBox.getCenter());
    }

    public void setAuthor(LivingEntity placer){
        this.authorName = placer.getEntityName();
    }

    public void updateStructure() {
        setStructure(buildStructureNbtFromWorld());
    }

    public ItemStack getContent() {
        ItemStack content = new ItemStack(Items.PIECE_STANDS.get(getPiece()));
        PieceContainer.writeStructureNbt(content, getStructureNbt());
        PieceContainer.writeColor(content, getColor());
        return content;
    }

    public void setContent(@NotNull ItemStack stack) {
        setColor(PieceContainer.getColor(stack));
    }

    public ItemStack clearContent() {
        ItemStack stack = getContent();

        // reset info based on item
        setColor(StandItem.DEFAULT_COLOR_INT);

        return stack;
    }

    public PieceStructureBlock getPieceStructureBlock() {
        if (getCachedState().getBlock() instanceof PieceStructureBlock pieceStructureBlock)
            return pieceStructureBlock;

        if (world == null)
            return null;

        return (PieceStructureBlock) world.getBlockState(pos).getBlock();
    }

    public Piece getPiece() {
        return getPieceStructureBlock().getPiece();
    }

    public Direction getFacing() {
        return PieceStructureBlock.getFacing(getCachedState());
    }

    private NbtCompound buildStructureNbtFromWorld() {
        StructureTemplate structureTemplate = new StructureTemplate();
        BlockBox box = getPieceStructureBlock().getBuildingBox(getCachedState(), pos);
        BlockPos start = new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ());
        structureTemplate.saveFromWorld(this.world, start, box.getDimensions().add(1, 1, 1), false, Blocks.AIR);
        // Note: author is not actually stored in structure NBT unfortunately (last checked in 1.19.4)
        if (authorName != null)
            structureTemplate.setAuthor(authorName);

        StructureHelper.rotate(structureTemplate, facingToRotation(getFacing()));

        NbtCompound structureNbt = structureTemplate.writeNbt(new NbtCompound());
        return structureNbt;
    }

    private static BlockRotation facingToRotation(Direction facing) {
        return switch (facing) {
            case DOWN -> throw new IllegalStateException("Invalid facing direction of PieceStructureBlock: " + facing);
            case UP -> throw new IllegalStateException("Invalid facing direction of PieceStructureBlock: " + facing);
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case SOUTH -> BlockRotation.NONE;
            case WEST -> BlockRotation.COUNTERCLOCKWISE_90;
            case EAST -> BlockRotation.CLOCKWISE_90;
        };
    }

    public int getPieceHeight() {
        return getPiece().getHeight();
    }

    public boolean isPowered() {
        if (!hasWorld())
            return false;
        return getWorld().isReceivingRedstonePower(getPos());
    }

    public boolean shouldShowOutline() {
        return !isPowered();
    }

    // From GameEventListener

    @Override
    public PositionSource getPositionSource() {
        return listenerSource;
    }

    @Override
    public int getRange() {
        // required to reach outermost corner for king which is 16 blocks high
        return 10;
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent event, GameEvent.Emitter emitter, Vec3d emitterPos) {
        if (!List.of(GameEvent.BLOCK_PLACE, GameEvent.BLOCK_CHANGE, GameEvent.BLOCK_DESTROY).contains(event))
            return false;

        BlockPos eventPos = BlockPos.ofFloored(emitterPos);
        if (!buildingBox.contains(eventPos))
            return false;

        // block break event gets called before the actual block break, so we delay by one
        int delay = event.equals(GameEvent.BLOCK_DESTROY) ? 1 : 0;
        world.scheduleBlockTick(pos, getPieceStructureBlock(), delay);
        return true;
    }

    public GameEventListener getEventListener() {
        return this;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (authorName != null)
            nbt.putString(AUTHOR_KEY, authorName);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(AUTHOR_KEY))
            this.authorName = nbt.getString(AUTHOR_KEY);
    }
}
