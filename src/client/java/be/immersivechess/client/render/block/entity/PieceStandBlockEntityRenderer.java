package be.immersivechess.client.render.block.entity;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.client.render.model.PieceModel;
import be.immersivechess.structure.StructureHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.profiler.Profiler;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class PieceStandBlockEntityRenderer <E extends StructureRenderedBlockEntity> implements BlockEntityRenderer<E> {

    private final BlockEntityRendererFactory.Context context;

    public PieceStandBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public void render(E entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        StructureTemplate structure = entity.getStructure();
        if (structure == null) return;

        Profiler profiler = context.getRenderDispatcher().world.getProfiler();
        profiler.push("stand");

        Map<BlockPos, BlockEntity> blockEntities = StructureHelper.buildBlockEntityMap(structure);

        // TODO: For stand only
        float scale = 1f / 16f;
        float rotation = entity.getCachedState().get(Properties.HORIZONTAL_FACING).asRotation();
        rotation += 180;

        matrices.push();
        // TODO: For stand only
        matrices.translate(0.25, 3f / 16f, 0.25);
        matrices.scale(scale, scale, scale);
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity be = entry.getValue();

            BlockEntityRenderer<BlockEntity> beRenderer = context.getRenderDispatcher().get(be);
            if (beRenderer == null)
                continue;

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation), 4f, 0, 4f);
            matrices.translate(pos.getX(), pos.getY(), pos.getZ());
            // Have blockEntities face the same way
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180), 0.5f, 0, 0.5f);

            // TODO: compute lighting based on structure
            beRenderer.render(be, tickDelta, matrices, vertexConsumers, light, overlay);
            matrices.pop();
        }
        matrices.pop();
        profiler.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(E blockEntity) {
        return false;
    }

    @Override
    public int getRenderDistance() {
        // reduce render distance, because everything is rendered smaller anyway
        return 16;
    }
}
