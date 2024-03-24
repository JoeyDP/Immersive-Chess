package be.immersivechess.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Shadow public abstract int getCount();

    @Inject(at = @At("RETURN"), method = "Lnet/minecraft/item/ItemStack;split(I)Lnet/minecraft/item/ItemStack;")
    private void split(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        // If a damaged stack is split, only retain the damage on the item that is left behind.
        if (stack.isDamaged() && getCount() > 0)
            stack.setDamage(0);
    }

}
