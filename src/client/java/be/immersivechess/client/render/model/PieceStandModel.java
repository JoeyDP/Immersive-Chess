package be.immersivechess.client.render.model;

import be.immersivechess.ImmersiveChess;
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
 * Piece with stand below. Used for placeholders that go in chess case and during building of pieces with piece structure block.
 */
@Environment(EnvType.CLIENT)
public class PieceStandModel implements UnbakedModel {

    private final Piece piece;

    public PieceStandModel(Piece piece){
        this.piece = piece;
    }

    private Identifier getPieceModelPath(){
        return new Identifier(ImmersiveChess.MOD_ID, "block/piece/" + piece.getIdentifier().getPath());
    }

    private Identifier getStandModelPath(){
        return new Identifier(ImmersiveChess.MOD_ID, "block/stand");
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of(getPieceModelPath(), getStandModelPath());
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
        modelLoader.apply(getStandModelPath());
        modelLoader.apply(getPieceModelPath());
    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        BakedModel pieceModel = baker.bake(getPieceModelPath(), rotationContainer);
        BakedModel standModel = baker.bake(getStandModelPath(), rotationContainer);
        return new BakedPieceStandModel(piece, pieceModel, standModel);
    }

    static class BakedPieceStandModel implements BakedModel, FabricBakedModel {
        private final Piece piece;
        private final BakedModel pieceModel;
        private final BakedModel standModel;

        private final ModelTransformation modelTransformation;

        private BakedPieceStandModel(Piece piece, BakedModel pieceModel, BakedModel standModel) {
            this.piece = piece;
            this.pieceModel = pieceModel;
            this.standModel = standModel;
            modelTransformation = createModelTransformation();
        }

        private ModelTransformation createModelTransformation() {
            ModelTransformation modelTransformation = TransformationHelper.getMutableDefaultModelTransform();

            modelTransformation.firstPersonLeftHand.translation.y += 0.1;
            modelTransformation.firstPersonRightHand.translation.y += 0.1;
            modelTransformation.thirdPersonLeftHand.translation.z += 0.1;
            modelTransformation.thirdPersonRightHand.translation.z += 0.1;

            modelTransformation.firstPersonLeftHand.scale.set(0.55f);
            modelTransformation.firstPersonRightHand.scale.set(0.55f);
            modelTransformation.thirdPersonLeftHand.scale.set(0.55f);
            modelTransformation.thirdPersonRightHand.scale.set(0.55f);

            modelTransformation.firstPersonLeftHand.rotation.y -= 90;
            modelTransformation.firstPersonRightHand.rotation.y += 90;

            modelTransformation.thirdPersonLeftHand.rotation.y += 90;
            modelTransformation.thirdPersonRightHand.rotation.y += 90;

//            modelTransformation.fixed.translation.y -= 0.2;
            modelTransformation.fixed.translation.z -= 0.1;

            modelTransformation.gui.scale.mul(1.15f);
            modelTransformation.gui.translation.y -= 0.05;
//            modelTransformation.gui.rotation.y -= 180;

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
            return false;
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
            return standModel.getParticleSprite();
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
            standModel.emitBlockQuads(blockView, state, blockPos, randomSupplier, renderContext);
            renderContext.pushTransform(new QuadTransform.Scale(0.5f));
            renderContext.pushTransform(new QuadTransform.Translate(0.5f, 3/8f, 0.5f));
            pieceModel.emitBlockQuads(blockView, state, blockPos, randomSupplier, renderContext);
            renderContext.popTransform();
            renderContext.popTransform();
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext renderContext) {
            standModel.emitItemQuads(stack, randomSupplier, renderContext);
            renderContext.pushTransform(new QuadTransform.Scale(0.5f));
            renderContext.pushTransform(new QuadTransform.Translate(0.5f, 3/8f, 0.5f));
            pieceModel.emitItemQuads(stack, randomSupplier, renderContext);
            renderContext.popTransform();
            renderContext.popTransform();
        }
    }
}
