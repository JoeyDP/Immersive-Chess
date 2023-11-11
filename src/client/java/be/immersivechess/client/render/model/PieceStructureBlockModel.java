package be.immersivechess.client.render.model;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.client.render.model.util.QuadTransform;
import be.immersivechess.client.render.model.util.TransformationHelper;
import be.immersivechess.logic.Piece;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Piece structure block with model of built piece above as trophy.
 */
@Environment(EnvType.CLIENT)
public class PieceStructureBlockModel implements UnbakedModel {
    private final Piece piece;

    public PieceStructureBlockModel(Piece piece){
        this.piece = piece;
    }

    private Identifier getPieceStandModelPath(){
        return new Identifier(ImmersiveChess.MOD_ID, "block/stand/" + piece.getIdentifier().getPath());
    }

    private Identifier getStructureBaseModelPath(){
        return new Identifier(ImmersiveChess.MOD_ID, "block/structure_base/" + piece.getIdentifier().getPath());
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of(getPieceStandModelPath(), getStructureBaseModelPath());
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        BakedModel baseModel = baker.bake(getStructureBaseModelPath(), rotationContainer);
        // piece stand model is rotated an additional 180 degrees to face the other direction
        BakedModel pieceStandModel = baker.bake(getPieceStandModelPath(), getRotatedBakeSettings(rotationContainer, 180));
        return new BakedPieceStructureBlockModel(piece, pieceStandModel, baseModel);
    }

    private static ModelBakeSettings getRotatedBakeSettings(ModelBakeSettings rotationContainer, int extraRotation){
        for (int y : new int[]{0, 90, 180, 270}){
            ModelRotation rotation = ModelRotation.get(0, y);
            if (rotation.getRotation().equals(rotationContainer.getRotation())){
                return ModelRotation.get(0, y + extraRotation);
            }
        }
        ImmersiveChess.LOGGER.error("Failed to rotate piece stand model within piece structure block");
        return rotationContainer;
    }

    static class BakedPieceStructureBlockModel implements BakedModel, FabricBakedModel {
        private final Piece piece;
        private final BakedModel pieceStandModel;
        private final BakedModel baseModel;

        private final ModelTransformation modelTransformation;

        private BakedPieceStructureBlockModel(Piece piece, BakedModel pieceStandModel, BakedModel baseModel) {
            this.piece = piece;
            this.pieceStandModel = pieceStandModel;
            this.baseModel = baseModel;
            modelTransformation = createModelTransformation();
        }

        private ModelTransformation createModelTransformation() {
            ModelTransformation modelTransformation = TransformationHelper.getMutableDefaultModelTransform();

            modelTransformation.firstPersonLeftHand.rotation.y -= 90;
            modelTransformation.firstPersonRightHand.rotation.y += 90;

            modelTransformation.thirdPersonLeftHand.rotation.y += 90;
            modelTransformation.thirdPersonRightHand.rotation.y += 90;

//            modelTransformation.fixed.translation.y -= 0.2;
            modelTransformation.fixed.translation.z -= 0.1;

//            modelTransformation.gui.scale.mul(0.67f);
            modelTransformation.gui.translation.y -= 0.15;

            // Knights are rotated to the right in itemframe
            if(piece == Piece.WHITE_KNIGHT || piece == Piece.BLACK_KNIGHT){
                modelTransformation.fixed.rotation.y += 90;
            }

            return modelTransformation;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
            return Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isBuiltin() {
            return false;
        }

        @Override
        public boolean hasDepth() {
            return true;
        }

        @Override
        public boolean isSideLit() {
            return true;
        }

        @Override
        public Sprite getParticleSprite() {
            return baseModel.getParticleSprite();
        }

        @Override
        public ModelTransformation getTransformation() {
            return modelTransformation;
        }

        @Override
        public ModelOverrideList getOverrides() {
            return ModelOverrideList.EMPTY;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos blockPos, Supplier<Random> randomSupplier, RenderContext renderContext) {
            baseModel.emitBlockQuads(blockView, state, blockPos, randomSupplier, renderContext);
            renderContext.pushTransform(new QuadTransform.Translate(0f, 13/16f, 0f));
            pieceStandModel.emitBlockQuads(blockView, state, blockPos, randomSupplier, renderContext);
            renderContext.popTransform();
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext renderContext) {
            baseModel.emitItemQuads(stack, randomSupplier, renderContext);
        }
    }
}
