package be.immersivechess.client.render.block.entity;

import be.immersivechess.block.PieceStructureBlock;
import be.immersivechess.block.entity.PieceStructureBlockEntity;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class PieceStructureBlockEntityRenderer <E extends PieceStructureBlockEntity> extends StructureRenderedBlockEntityRenderer<E> {

    public PieceStructureBlockEntityRenderer(BlockEntityRendererFactory.Context context){
        super(context);
    }

    @Override
    public void render(E entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.shouldShowOutline())
            renderOutline(entity, matrices, vertexConsumers);

        matrices.push();
        matrices.translate(0.25, 1f, 0.25);
        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180), 0.5f, 0, 0.5f);
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }

    public void renderOutline(PieceStructureBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers){
        float outlineColorUniform = switch(entity.getPiece().getColor()){
            case BLACK -> 0f;
            case WHITE -> 1f;
        };

        Box box = new Box(0, 0, 0, PieceStructureBlock.PIECE_BASE_SIZE, entity.getPieceHeight(), PieceStructureBlock.PIECE_BASE_SIZE);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        float rotation = -(float)Math.toRadians(entity.getFacing().asRotation());

        matrices.push();
        // rotation
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(new Quaternionf().rotationY(rotation));
        matrices.translate(-0.5, -0.5, -0.5);
        // translation
        matrices.translate(0, 0, 1);
        WorldRenderer.drawBox(matrices, vertexConsumer, box, outlineColorUniform, outlineColorUniform, outlineColorUniform, 1.0f);
        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(PieceStructureBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 32;
    }
}
