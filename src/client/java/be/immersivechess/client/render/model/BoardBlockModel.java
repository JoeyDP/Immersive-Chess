package be.immersivechess.client.render.model;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.client.render.model.util.QuadTransform;
import ch.astorm.jchess.core.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class BoardBlockModel implements UnbakedModel {

    private static final Map<Color, SpriteIdentifier> spriteIdentifiers = Map.of(
            Color.BLACK, new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ImmersiveChess.MOD_ID, "block/board_black")),
            Color.WHITE, new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier(ImmersiveChess.MOD_ID, "block/board_white"))
    );

    private final Color color;

    public BoardBlockModel(Color color) {
        this.color = color;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {

    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Sprite sprite = textureGetter.apply(spriteIdentifiers.get(color));

        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        MeshBuilder builder = renderer.meshBuilder();
        QuadEmitter emitter = builder.getEmitter();

        for (Direction direction : Direction.values()) {
            emitter.square(direction, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
            emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
            emitter.color(-1, -1, -1, -1);
            emitter.emit();
        }

        Mesh defaultMesh = builder.build();

        return new BoardBlockBakedModel(sprite, defaultMesh);
    }

    static class BoardBlockBakedModel implements BakedModel, FabricBakedModel {
        private final Sprite particles;

        private final Mesh defaultMesh;

        private BoardBlockBakedModel(Sprite particles, Mesh defaultMesh) {
            this.particles = particles;
            this.defaultMesh = defaultMesh;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        private void emitAppearanceState(BlockState appearanceState, QuadEmitter emitter, Random random) {
            MaterialFinder materialFinder = RendererAccess.INSTANCE.getRenderer().materialFinder();
            materialFinder.blendMode(appearanceState.isOpaque() ? BlendMode.SOLID : BlendMode.TRANSLUCENT);
            RenderMaterial material = materialFinder.find();
            BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(appearanceState);
            QuadTransform tintRemap = new QuadTransform.TintRemap(appearanceState);

            for (Direction direction : Stream.concat(Arrays.stream(Direction.values()), Stream.of((Direction) null)).toList()) {
                for (BakedQuad quad : model.getQuads(appearanceState, direction, random)) {
                    emitter.fromVanilla(quad, material, direction);
                    tintRemap.transform(emitter);
                    emitter.emit();
                }
            }
        }

        @Override
        public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
//            ImmersiveChess.LOGGER.info("emitting block Quads");
            BlockState appearanceState = getAppearanceBlockState(blockView, pos);
            if (appearanceState == null || appearanceState.getBlock() == Blocks.BOARD_BLOCK) {
                context.meshConsumer().accept(defaultMesh);
                return;
            }

            emitAppearanceState(appearanceState, context.getEmitter(), randomSupplier.get());
        }

        private @Nullable BlockState getAppearanceBlockState(BlockRenderView blockView, BlockPos blockPos) {
            Object entityData = ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(blockPos);
            if (entityData instanceof BlockState appearanceBlockState) {
                return appearanceBlockState;
            }
            return null;
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
            context.meshConsumer().accept(defaultMesh);
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
        public boolean hasDepth() {
            return true;
        }

        @Override
        public boolean isSideLit() {
            return true;
        }

        @Override
        public boolean isBuiltin() {
            return false;
        }

        @Override
        public Sprite getParticleSprite() {
            return particles;
        }

        @Override
        public ModelTransformation getTransformation() {
            return ModelHelper.MODEL_TRANSFORM_BLOCK;
        }

        @Override
        public ModelOverrideList getOverrides() {
            return ModelOverrideList.EMPTY;
        }

    }
}
