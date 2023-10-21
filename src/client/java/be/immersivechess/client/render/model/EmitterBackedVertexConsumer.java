package be.immersivechess.client.render.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;

public class EmitterBackedVertexConsumer extends FixedColorVertexConsumer {
    int index = 0;
    QuadEmitter emitter;

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
        // TODO: figure out overlay
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
            emitter.emit();
        }
        index = (index + 1) % 4;
    }
}
