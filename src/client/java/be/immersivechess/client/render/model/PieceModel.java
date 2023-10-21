package be.immersivechess.client.render.model;

import be.immersivechess.client.structure.StructureResolver;
import be.immersivechess.logic.Piece;
import be.immersivechess.structure.StructureHelper;
import com.google.common.collect.MapMaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class PieceModel implements UnbakedModel {

    private final Piece piece;

    private final SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("minecraft:block/stone"));

    public PieceModel(Piece piece) {
        this.piece = piece;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        // This model technically depends on all other models, but we only build the mesh at a later stage so this can be empty.
        return Collections.emptyList();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Sprite sprite = textureGetter.apply(spriteIdentifier);

        return new BakedPieceModel(piece, sprite, rotationContainer);
    }

    static class BakedPieceModel implements BakedModel, FabricBakedModel {
        private final Piece piece;
        private final Sprite particles;
        private final ModelBakeSettings rotationContainer;
        private static final float SCALE = 1f / 8f;

        private final ModelTransformation modelTransformation;

        private final Map<StructureTemplate, Mesh> meshCache = new MapMaker().weakKeys().makeMap();

        private BakedPieceModel(Piece piece, Sprite particles, ModelBakeSettings rotationContainer) {
            this.piece = piece;
            this.particles = particles;
            this.rotationContainer = rotationContainer;
            modelTransformation = createModelTransformation();
        }

        private ModelTransformation createModelTransformation() {
            ModelTransformation modelTransformation = TransformationHelper.getMutableDefaultModelTransform();

            modelTransformation.firstPersonLeftHand.rotation.y -= 225;
            modelTransformation.firstPersonRightHand.rotation.y -= 45;

            modelTransformation.thirdPersonLeftHand.rotation.y -= 45;
            modelTransformation.thirdPersonRightHand.rotation.y -= 45;

            modelTransformation.fixed.translation.y -= 0.2;
            modelTransformation.fixed.translation.z -= 0.1;

            modelTransformation.gui.scale.mul(0.67f);
            modelTransformation.gui.translation.y -= 0.15;

            // Knights are rotated to the right in itemframe
            if (piece == Piece.WHITE_KNIGHT || piece == Piece.BLACK_KNIGHT) {
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
            return particles;
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
            StructureTemplate structure = getStructure(blockView, blockPos);

            if (structure == null)
                return;

            Mesh mesh = getOrCreateMesh(structure, blockView, randomSupplier);
            mesh.outputTo(renderContext.getEmitter());
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext renderContext) {
            StructureTemplate structure = getStructure(stack);

            if (structure == null)
                return;

            Mesh mesh = getOrCreateMesh(structure, MinecraftClient.getInstance().world, randomSupplier);
            mesh.outputTo(renderContext.getEmitter());
        }

        @Nullable
        private StructureTemplate getStructure(BlockRenderView blockView, BlockPos blockPos) {
            // Nbt of structure is passed
            Object entityData = blockView.getBlockEntityRenderData(blockPos);

            // null or unknown type -> return null which is empty structure
            if (!(entityData instanceof NbtCompound structureNbt))
                return null;

            // empty nbt is also rendered as empty
            if (structureNbt.isEmpty())
                return null;

            return StructureResolver.getStructure(structureNbt);
        }

        private StructureTemplate getStructure(ItemStack itemStack) {
            return StructureResolver.getStructure(itemStack);
        }

        private Mesh getOrCreateMesh(StructureTemplate structure, BlockRenderView worldBlockView, Supplier<Random> randomSupplier) {
            return meshCache.computeIfAbsent(structure, nbt -> createMesh(nbt, worldBlockView, randomSupplier));
        }

        private Mesh createMesh(StructureTemplate structure, BlockRenderView worldBlockView, Supplier<Random> randomSupplier) {
//            ImmersiveChess.LOGGER.info("creating new mesh for piece " + piece);
//            ImmersiveChess.LOGGER.info("cache size " + meshCache.size());

            net.minecraft.util.math.random.Random random = randomSupplier.get();
            RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder()
                    .blendMode(BlendMode.TRANSLUCENT)
                    .ambientOcclusion(TriState.DEFAULT)
                    .emissive(false)
                    .find();
            BlockModels blockModels = MinecraftClient.getInstance().getBakedModelManager().getBlockModels();

            AffineTransformation affineTransformation = rotationContainer.getRotation();
            QuadTransform rotationTransform = new QuadTransform.Rotate(affineTransformation.getLeftRotation());
            QuadTransform scaleTransform = new QuadTransform.Scale(SCALE);
            QuadTransform materialTransform = new QuadTransform() {
                @Override
                public boolean transform(MutableQuadView quad) {
                    quad.material(material);
                    return true;
                }
            };

            // TODO: provide block entities from structure
            Map<BlockPos, BlockState> blockStates = StructureHelper.buildBlockStateMap(structure);
            BlockRenderView world = new MiniatureBlockRenderView(blockStates);
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            MeshBuilder builder = renderer.meshBuilder();
            QuadEmitter emitter = builder.getEmitter();

            for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState bs = entry.getValue();

                QuadTransform translateTransform = new QuadTransform.Translate(pos.getX(), pos.getY(), pos.getZ());
                QuadTransform tintTransform = new QuadTransform.TintRemap(bs);

                BlockRenderContext renderContext = new BlockRenderContext() {
                    protected void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
                        // Take the processed quad and add it to the mesh.
                        emitter.copyFrom(quad);
                        emitter.cullFace(null);
                        // setting cullFace to null also sets nominalFace (this might be a bug in the renderer)
                        emitter.nominalFace(quad.nominalFace());

                        tintTransform.transform(emitter);
                        materialTransform.transform(emitter);
                        translateTransform.transform(emitter);
                        scaleTransform.transform(emitter);
                        rotationTransform.transform(emitter);

                        emitter.emit();
                    }
                };

                // TODO: Could add support for blockState appearances, which may differ from actual block states (facades etc.)
                //  However, they depend on direction
//                bs = bs.getAppearance(world, pos, direction, bs, null);

                BakedModel model = blockModels.getModel(bs);

                // Have RenderContext perform most of the rendering, we intercept the result in the overide of "bufferQuad" above
                renderContext.render(world, model, bs, pos, new MatrixStack(), null, true, random, 0, OverlayTexture.DEFAULT_UV);
            }

            return builder.build();
        }
    }
}
