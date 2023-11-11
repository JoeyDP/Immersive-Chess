package be.immersivechess.client.render.model.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class TransformationHelper {
    public static Transformation copyTransformation(Transformation transformation){
        return new Transformation(transformation.rotation, transformation.translation, transformation.scale);
    }

    // Model transformations for items
    public static ModelTransformation getMutableDefaultModelTransform() {
        return new ModelTransformation(
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT),
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT),
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_LEFT),
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_RIGHT),
                copyTransformation(Transformation.IDENTITY),                // head
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_GUI),        // gui
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_GROUND),     // ground
                copyTransformation(ModelHelper.TRANSFORM_BLOCK_FIXED)       // fixed
        );
    }

}
