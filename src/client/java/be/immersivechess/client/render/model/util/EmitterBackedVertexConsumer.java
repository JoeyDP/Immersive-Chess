package be.immersivechess.client.render.model.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class EmitterBackedVertexConsumer extends FixedColorVertexConsumer implements TransformStack {
    int index = 0;
    QuadEmitter emitter;

    private final ObjectArrayList<RenderContext.QuadTransform> transformStack = new ObjectArrayList<>();

    @Override
    public ObjectArrayList<RenderContext.QuadTransform> getPostTransformStack(){
        return transformStack;
    }

    public EmitterBackedVertexConsumer(QuadEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        emitter.pos(index, (float) x, (float) y, (float) z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        int col = ColorHelper.Argb.getArgb(alpha, red, green, blue);
        emitter.color(index, col);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        emitter.uv(index, u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        // Overlay is not used by fluids (as of the writing of this code) -> not implemented
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        emitter.lightmap(index, LightmapTextureManager.pack(u, v));
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        emitter.normal(index, x, y, z);
        return this;
    }

    @Override
    public void next() {
        if (index == 3){
            emit();
        }
        index = (index + 1) % 4;
    }

    private void emit(){
        postTransform(emitter);
        emitter.emit();
    }
}