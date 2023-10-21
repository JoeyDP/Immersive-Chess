package be.immersivechess.client.render.model;

import be.immersivechess.client.mixin.BlockRenderInfoAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.util.math.Direction;

public class ForwardingEmitter extends MutableQuadViewImpl {

    private final QuadEmitter emitter;
    private final BlockRenderInfo blockRenderInfo;

    private ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();


    public ForwardingEmitter(QuadEmitter emitter, BlockRenderInfo blockRenderInfo){
        this.emitter = emitter;
        this.blockRenderInfo = blockRenderInfo;
        data = new int[256];
    }

    public void pushTransform(QuadTransform transform){
        transformStack.add(transform);
    }

    public void popTransform(){
        transformStack.pop();
    }

    @Override
    public void emitDirectly() {
        Direction direction = cullFace();
        if (!((BlockRenderInfoAccess)blockRenderInfo).invokeShouldDrawFace(direction))
            return;

        computeGeometry();
        emitter.copyFrom(this);

        int i = transformStack.size() - 1;
        while (i >= 0) {
            if (!transformStack.get(i--).transform(emitter)) {
                break;
            }
        }
        emitter.emit();
    }
}
