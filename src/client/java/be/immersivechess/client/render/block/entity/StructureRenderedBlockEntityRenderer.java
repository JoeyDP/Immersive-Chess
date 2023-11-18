package be.immersivechess.client.render.block.entity;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.entity.PieceStructureBlockEntity;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.client.ImmersiveChessClient;
import be.immersivechess.client.render.model.PieceModel;
import be.immersivechess.structure.StructureHelper;
import be.immersivechess.world.MiniatureWorld;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;

import java.util.Map;

/**
 * Dynamic renderer used only for the block entities. Other parts are rendered once in model and added to world mesh.
 */
@Environment(EnvType.CLIENT)
public class StructureRenderedBlockEntityRenderer<E extends StructureRenderedBlockEntity> implements BlockEntityRenderer<E> {

    protected final BlockEntityRendererFactory.Context context;

    public StructureRenderedBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public void render(E entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        MiniatureWorld miniWorld = entity.getMiniWorld();
        if (miniWorld == null) return;
        if (!entity.hasWorld()) return;

        Profiler profiler = context.getRenderDispatcher().world.getProfiler();
        profiler.push("miniature_be");

        // Due to emissive rendering, a block light of 15 is always provided to this function iso the actual value.
        // So we compute it from the world
        int globalBlockLight = entity.getWorld().getLightLevel(LightType.BLOCK, entity.getPos());
        int globalSkyLight = LightmapTextureManager.getSkyLightCoordinates(light);

        Direction dir = entity.getCachedState().get(Properties.HORIZONTAL_FACING);
        float rotation = dir.asRotation();
        rotation += 180;

        matrices.push();
        matrices.scale(PieceModel.SCALE, PieceModel.SCALE, PieceModel.SCALE);

        Map<BlockPos, BlockEntity> blockEntities = miniWorld.getBlockEntities();
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity be = entry.getValue();

            BlockEntityRenderer<BlockEntity> beRenderer = context.getRenderDispatcher().get(be);
            if (beRenderer == null) continue;
            if (beRenderer instanceof StructureRenderedBlockEntityRenderer) continue;      // no recursive block entities

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation), 4f, 0, 4f);
            matrices.translate(pos.getX(), pos.getY(), pos.getZ());

            // Most BlockEntityRenderers don't do parts of their logic if the world is not present, though they often don't use it.
            // TODO: provide miniature world
//            be.setWorld(miniWorld);

            int localBlockLight = entity.getMiniWorld().getLightLevel(LightType.BLOCK, pos);
            int localSkyLight = entity.getMiniWorld().getLightLevel(LightType.SKY, pos);
            // Block light is based on max available. Skylight is propagated further.
            int localLight = LightmapTextureManager.pack(Math.max(localBlockLight, globalBlockLight), Math.max(0, localSkyLight - 15 + globalSkyLight));

            beRenderer.render(be, tickDelta, matrices, vertexConsumers, localLight, overlay);
            matrices.pop();

//            be.setWorld(null);
        }
        matrices.pop();
        profiler.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(E blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        // reduce render distance, because everything is rendered smaller anyway
        return 16;
    }
}
