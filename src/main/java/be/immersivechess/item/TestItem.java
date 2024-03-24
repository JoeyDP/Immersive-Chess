package be.immersivechess.item;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.mixin.SettingsAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TestItem extends BlockItem {

    public static Settings modifySettings(Settings settings){
        ((SettingsAccessor) settings).setMaxDamage(128);
        return settings;
    }

    public TestItem(Block block, Settings settings) {
        super(block, modifySettings(settings));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ImmersiveChess.LOGGER.info("damaging item");
        ItemStack stack = user.getStackInHand(hand);
        stack.damage(1, user, playerEntity -> {});

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        ItemStack stack = context.getStack();
        return super.canPlace(context, state) && (stack.getCount() > 1 || stack.getDamage() == 0);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        // TODO: when stack split, don't copy damage value
        // TODO: Also when dropping (q) remove damage value when needed
        ImmersiveChess.LOGGER.info("Clicked stack" + clickType.toString());
        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        ImmersiveChess.LOGGER.info("stack clicked" + clickType.toString());
        return super.onStackClicked(stack, slot, clickType, player);
    }

}
