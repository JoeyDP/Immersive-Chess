package be.immersivechess.client.render.model.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

public interface TransformStack {

    ObjectArrayList<RenderContext.QuadTransform> getPostTransformStack();

    default void pushPostTransform(RenderContext.QuadTransform transform){
        getPostTransformStack().add(transform);
    }

    default void popPostTransform(){
        getPostTransformStack().pop();
    }

    // Cannot be called transform because of final implementation in AbstractRenderContext
    default boolean postTransform(MutableQuadView emitter){
        int i = getPostTransformStack().size() - 1;
        while (i >= 0) {
            if (!getPostTransformStack().get(i--).transform(emitter)) {
                return false;
            }
        }
        return true;
    }
}
