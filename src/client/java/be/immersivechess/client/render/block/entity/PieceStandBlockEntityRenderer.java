package be.immersivechess.client.render.block.entity;

import be.immersivechess.block.entity.PieceStandBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class PieceStandBlockEntityRenderer<E extends PieceStandBlockEntity> extends StructureRenderedBlockEntityRenderer<E> {
    public PieceStandBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(E entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.25, 3f / 16f, 0.25);
        matrices.scale(0.5f, 0.5f, 0.5f);
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(E blockEntity) {
        return false;
    }
}
