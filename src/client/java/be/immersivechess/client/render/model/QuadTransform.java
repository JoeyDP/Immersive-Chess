package be.immersivechess.client.render.model;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.StandBlock;
import be.immersivechess.block.entity.StructureRenderedBlockEntity;
import be.immersivechess.client.color.TintMapper;
import be.immersivechess.item.PieceContainer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

public abstract class QuadTransform implements RenderContext.QuadTransform {
    private static abstract class PositionTransform extends QuadTransform {
        @Override
        public boolean transform(MutableQuadView quad) {
            Vector3f p = new Vector3f();
            for (int i = 0; i < 4; i++) {
                quad.copyPos(i, p);
                apply(p);
                quad.pos(i, p);
            }
            return true;
        }

        /**
         * Applied for all vertices in the quad
         */
        abstract protected void apply(Vector3f pos);
    }

    public static class Scale extends PositionTransform {
        private final float scale;

        public Scale(float scale) {
            this.scale = scale;
        }

        @Override
        protected void apply(Vector3f p) {
            p.mul(scale);
        }
    }

    public static class Translate extends PositionTransform {
        private final Vector3f offset;

        public Translate(Vector3f offset) {
            this.offset = offset;
        }

        public Translate(float x, float y, float z) {
            this.offset = new Vector3f(x, y, z);
        }

        @Override
        protected void apply(Vector3f p) {
            p.add(offset);
        }
    }

    private static abstract class RotationTransform extends PositionTransform {
        @Override
        public boolean transform(MutableQuadView quad) {
            Vector3f p = new Vector3f();
            for (int i = 0; i < 4; i++) {
                quad.copyPos(i, p);
                apply(p);
                quad.pos(i, p);

                // also transform normals
                if (quad.hasNormal(i)) {
                    quad.copyNormal(i, p);
                    applyNormal(p);
                    quad.normal(i, p);
                }
            }
            return true;
        }

        abstract protected void applyNormal(Vector3f pos);
    }

    public static class Rotate extends RotationTransform {
        private final Matrix3f rotation;
        private final Vector3f center;

        public Rotate(Quaternionf rotation) {
            this(rotation, new Vector3f(0.5f, 0.5f, 0.5f));
        }

        public Rotate(Quaternionf rotation, Vector3f center) {
            this.rotation = rotation.get(new Matrix3f());
            this.center = center;
        }

        @Override
        public boolean transform(MutableQuadView quad) {
            // also update nominalFace
            Direction d = quad.nominalFace();
            if (d != null) {
                Vector3f newFacing = d.getUnitVector().mul(rotation);
                quad.nominalFace(Direction.getFacing(newFacing.x, newFacing.y, newFacing.z));
            }
            return super.transform(quad);
        }

        @Override
        protected void apply(Vector3f p) {
            p.sub(center);
            p.mul(rotation);
            p.add(center);
        }

        @Override
        protected void applyNormal(Vector3f n) {
            n.mul(rotation);
        }
    }

    public static class TintRemap extends QuadTransform {
        private final BlockState bs;

        public TintRemap(BlockState bs) {
            this.bs = bs;
        }

        @Override
        public boolean transform(MutableQuadView quad) {
            int tintIndex = quad.colorIndex();

            if (tintIndex < 0)
                return true;

            // Don't remap recursively (PieceContainer indicates StructureRenderedBlockEntity in this case)
            if (bs.getBlock() instanceof PieceContainer){
                // Don't apply dye again
                if (tintIndex < TintMapper.CAPACITY){
                    quad.colorIndex(-1);
                }
                return true;
            }

            // we do not expect the tintIndex to exceed our allocated capacity.
            if (tintIndex > TintMapper.CAPACITY)
                ImmersiveChess.LOGGER.warn("tintIndex exceeds allocated capacity. Some colors may get translated wrong.");

            Optional<Integer> offset = TintMapper.INSTANCE.getTintOffset(bs.getBlock());
            if (offset.isPresent()) {
                quad.colorIndex(offset.get() + tintIndex);
            } else {
                ImmersiveChess.LOGGER.warn("ColorProvider not found for Block " + bs.getBlock());
                quad.colorIndex(-1);
            }
            return true;
        }
    }

}
