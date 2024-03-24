package be.immersivechess.mixin;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.Settings.class)
public interface SettingsAccessor {
        @Accessor("maxDamage")
        public void setMaxDamage(int maxDamage);
}
