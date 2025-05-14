package be.immersivechess.client.render.model.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.client.render.VertexConsumer;

public class EmitterBackedBlockRenderContext extends BlockRenderContext implements TransformStack {

    private final QuadEmitter emitter;

    private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();

    public EmitterBackedBlockRenderContext(QuadEmitter emitter){
        this.emitter = emitter;
    }

    @Override
    public ObjectArrayList<RenderContext.QuadTransform> getPostTransformStack(){
        return transformStack;
    }

    /**
     * Have RenderContext perform most of the rendering, we intercept the result in the override of "bufferQuad"
     */
    @Override
    protected void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        // Take the processed quad and add it to the mesh.
        emitter.copyFrom(quad);
        emitter.cullFace(null);
        // setting cullFace to null also sets nominalFace (this might be a bug in the renderer)
        emitter.nominalFace(quad.nominalFace());

        // We only want to transform after most of the rendering
        // postTransform is our own transform stack. Default transform function needs to not do anything.
        postTransform(emitter);

        emitter.emit();
    }

}
