package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.mixin.MixinSettingsAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Objects;

public class TestItem extends BlockItem {

    public static Settings modifySettings(Settings settings){
        ((MixinSettingsAccessor) settings).setMaxDamage(32);
        return settings;
    }

    public TestItem(Block block, Settings settings) {
        super(block, modifySettings(settings));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        stack.damage(1, user, playerEntity -> {});

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        ItemStack stack = context.getStack();
        return super.canPlace(context, state) && (stack.getCount() > 1 || stack.getDamage() == 0);
    }

    private static boolean nbtEqualExceptDamage(ItemStack stack, ItemStack otherStack){
        NbtCompound nbt1 = stack.hasNbt() ? stack.getNbt().copy() : new NbtCompound();
        NbtCompound nbt2 = otherStack.hasNbt() ? otherStack.getNbt().copy() : new NbtCompound();
        nbt1.remove(ItemStack.DAMAGE_KEY);
        nbt2.remove(ItemStack.DAMAGE_KEY);

        return Objects.equals(nbt1, nbt2);
    }

    private static void updateStackWithUnits(ItemStack stack, int units){
        // needs to remain before setCount
        int remainder = units % stack.getMaxDamage();
        stack.setCount(units / stack.getMaxDamage());
        if (remainder > 0){
            stack.increment(1);
            stack.setDamage(stack.getMaxDamage() - remainder);
        }
    }

    @Override
    public boolean onClicked(ItemStack slotStack, ItemStack handStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        // TODO: Quick stack takes everything with same damage value together rather than merging them

        // Let default code handle it
        if (!slotStack.isOf(handStack.getItem()))
            return false;
        if (slotStack.isEmpty() || handStack.isEmpty())
            return false;
        if (ItemStack.canCombine(slotStack, handStack))
            return false;
        if (!nbtEqualExceptDamage(slotStack, handStack))
            return false;

        // try combining stacks
        int unitsPerCount = slotStack.getMaxDamage();
        int sourceUnits = unitsPerCount * (clickType == ClickType.LEFT ? handStack.getCount() : 1) - handStack.getDamage();
        int targetUnits = unitsPerCount * slotStack.getCount() - slotStack.getDamage();

        int targetSpaceUnits = unitsPerCount * slotStack.getMaxCount() - targetUnits;
        int transferredUnits = Math.min(sourceUnits, targetSpaceUnits);

        updateStackWithUnits(handStack, sourceUnits - transferredUnits);
        updateStackWithUnits(slotStack, targetUnits + transferredUnits);

        return true;
    }

}
