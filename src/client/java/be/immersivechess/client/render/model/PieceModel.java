package be.immersivechess.client.render.model;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.client.structure.StructureResolver;
import be.immersivechess.logic.Piece;
import be.immersivechess.structure.StructureHelper;
import com.google.common.collect.MapMaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
        private static final boolean CULL = true;
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
            Object entityData = ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(blockPos);

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
            RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.TRANSLUCENT).ambientOcclusion(TriState.FALSE).find();
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
            BlockRenderView world = createBlockRenderView(worldBlockView, blockStates);
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            MeshBuilder builder = renderer.meshBuilder();
            QuadEmitter emitter = builder.getEmitter();

//            VertexConsumer vertexConsumer = new EmitterBackedVertexConsumer(emitter);

            BlockRenderInfo blockRenderInfo = new BlockRenderInfo();
            blockRenderInfo.prepareForWorld(world, true);

            ForwardingEmitter myEmitter = new ForwardingEmitter(emitter, blockRenderInfo);

            BlockRenderContext renderContext = new BlockRenderContext() {
                @Override
                public QuadEmitter getEmitter() {
                    return myEmitter;
                }

                @Override
                public QuadEmitter getVanillaModelEmitter() {
                    return myEmitter;
                }
            };

            myEmitter.pushTransform(rotationTransform);
            myEmitter.pushTransform(scaleTransform);

            for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState bs = entry.getValue();

                QuadTransform translateTransform = new QuadTransform.Translate(pos.getX(), pos.getY(), pos.getZ());
                QuadTransform tintTransform = new QuadTransform.TintRemap(bs);

//                for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
//                    Direction direction = ModelHelper.faceFromIndex(i);

                // update to blockState appearances, which may differ from actual block states (facades etc.)
//                    bs = bs.getAppearance(world, pos, direction, bs, null);
                BakedModel model = blockModels.getModel(bs);
//
//                    if (direction != null) {
//                        // culling is possible
//                        BlockPos neighbourPos = pos.offset(direction);
//                        if (CULL && !Block.shouldDrawSide(bs, world, pos, direction, neighbourPos)) continue;
//                    }

                myEmitter.pushTransform(translateTransform);
                myEmitter.pushTransform(materialTransform);
                myEmitter.pushTransform(tintTransform);

                // TODO: issue when rendering to emitter, it applies color and doesn't supply tintIndex anymore
//              renderContext.render(world, model, bs, pos, new MatrixStack(), vertexConsumer, true, random, 0, OverlayTexture.DEFAULT_UV);

                blockRenderInfo.prepareForBlock(bs, pos, model.useAmbientOcclusion());

                model.emitBlockQuads(world, bs, pos, randomSupplier, renderContext);

                myEmitter.popTransform();
                myEmitter.popTransform();
                myEmitter.popTransform();

//                    for (BakedQuad quad : model.getQuads(bs, direction, random)) {
//                        emitter.fromVanilla(quad, material, null);       //  set cullFace to null because quads are not guaranteed to be on a face (not full block)
//
//                        translateTransform.transform(emitter);
//                        scaleTransform.transform(emitter);
//                        rotationTransform.transform(emitter);
//                        tintTransform.transform(emitter);
//
//                        emitter.emit();
//                    }
            }
//            }

            myEmitter.popTransform();
            myEmitter.popTransform();

            return builder.build();
        }

        private BlockRenderView createBlockRenderView(BlockRenderView worldBlockView, Map<BlockPos, BlockState> blockStates) {
            // TODO: extract class and turn into RenderBlockView that extends FabricBlockView
            // TODO: when complete, should no longer need worldBlockView
            return new RenderAttachedBlockView() {

                @Override
                public float getBrightness(Direction direction, boolean shaded) {
                    // Brightness gets applied again when rendering to world, and we don't want it twice
                    return 1;
//                    return worldBlockView.getBrightness(direction, shaded);
                }

                @Override
                public LightingProvider getLightingProvider() {
                    // TODO: this is not correct
                    return worldBlockView.getLightingProvider();

//                    RenderAttachedBlockView view = this;
//
//                    return new LightingProvider(new ChunkProvider() {
//                        @Nullable
//                        @Override
//                        public LightSourceView getChunk(int chunkX, int chunkZ) {
//                            return null;
//                        }
//
//                        @Override
//                        public BlockView getWorld() {
//                            return view;
//                        }
//                    }, true, true){
//                        @Override
//                        public int getLight(BlockPos pos, int ambientDarkness) {
//                            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
//                        }
//                    };
                }

                @Override
                public int getColor(BlockPos pos, ColorResolver colorResolver) {
//                    ImmersiveChess.LOGGER.warn("got unexpected call to getColor. Likely wrong value was returned");
                    return ColorHelper.Argb.getArgb(255, 255, 255, 255);
//                    return GrassColors.getDefaultColor();
//                    return worldBlockView.getColor(pos, colorResolver);
                }

                @Override
                public int getHeight() {
                    return 16;
                }

                @Override
                public int getBottomY() {
                    return 0;
                }

                @Nullable
                @Override
                public BlockEntity getBlockEntity(BlockPos pos) {
                    // TODO
                    return null;
                }

                @Override
                public BlockState getBlockState(BlockPos pos) {
                    return blockStates.getOrDefault(pos, Blocks.VOID_AIR.getDefaultState());
                }

                @Override
                public FluidState getFluidState(BlockPos pos) {
                    return Fluids.EMPTY.getDefaultState();
                }
            };
        }

    }
}
