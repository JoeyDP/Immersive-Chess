package be.immersivechess.client.mixin;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockRenderInfo.class)
public interface BlockRenderInfoAccess {

    @Invoker("shouldDrawFace")
    boolean invokeShouldDrawFace(Direction d);
}

