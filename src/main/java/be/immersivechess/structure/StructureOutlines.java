package be.immersivechess.structure;

import be.immersivechess.world.MiniatureBlockRenderView;
import com.google.common.collect.MapMaker;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

public class StructureOutlines {
    private static final Map<StructureTemplate, List<VoxelShape>> CACHE = new MapMaker().weakKeys().makeMap();

    private static final float SCALE = 1f / 8f;

    public static Optional<VoxelShape> getOrCreateOutline(StructureTemplate structure, Direction facing) {
        List<VoxelShape> outline = CACHE.get(structure);
        if (outline != null) return Optional.of(outline.get(facing.getHorizontal()));

        // if not present, submit compute job
        computeOutline(structure);
        return Optional.empty();
    }

    public static VoxelShape scale(VoxelShape original, float scale) {
        return original
                .getBoundingBoxes().stream()
                .map(box -> VoxelShapes.cuboid(
                                box.minX * scale, box.minY * scale, box.minZ * scale,
                                box.maxX * scale, box.maxY * scale, box.maxZ * scale
                        )
                )
                .reduce(VoxelShapes.empty(), (first, second) -> VoxelShapes.combine(first, second, BooleanBiFunction.OR));
    }

    private static VoxelShape rotate(VoxelShape original, Direction facing) {
        // only works for voxelshapes in a unit box
        Vector3d center = new Vector3d(0.5, 0, 0.5);
        Quaternionf rotation = RotationAxis.NEGATIVE_Y.rotationDegrees((facing.asRotation() + 180) % 360);

        return original
                .offset(-center.x, 0, -center.z)
                .getBoundingBoxes().stream()
                .map(box -> VoxelShapes.cuboid(new Box(
                                new Vec3d(rotation.transform(new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ))),
                                new Vec3d(rotation.transform(new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ)))
                        )
                ))
                .reduce(VoxelShapes.empty(), (first, second) -> VoxelShapes.combine(first, second, BooleanBiFunction.OR))
                .offset(center.x, 0, center.z);
    }

    private static void computeOutline(StructureTemplate structure) {
        Util.getMainWorkerExecutor().submit(() -> cacheOutlines(structure));
    }

    private static void cacheOutlines(StructureTemplate structure) {
        VoxelShape outline = createOutline(structure);

        List<VoxelShape> orientations = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            Direction facing = Direction.fromHorizontal(i);
            orientations.add(rotate(outline, facing));
        }

        CACHE.put(structure, orientations);
    }


    private static VoxelShape createOutline(StructureTemplate structure) {
        VoxelShape shape = VoxelShapes.empty();

        Map<BlockPos, BlockState> blockStateMap = StructureHelper.buildBlockStateMap(structure);
        BlockRenderView world = new MiniatureBlockRenderView(blockStateMap, Collections.emptyMap());

        for (Map.Entry<BlockPos, BlockState> entry : blockStateMap.entrySet()) {
            BlockState state = entry.getValue();
            if (state.isAir()) continue;

            BlockPos blockPos = entry.getKey();

            Vec3d offset = Vec3d.of(blockPos);
            VoxelShape[] transformed = state.getOutlineShape(world, blockPos)
                    .offset(offset.x, offset.y, offset.z)
                    .getBoundingBoxes().stream()
                    .map(box -> VoxelShapes.cuboid(
                                    box.minX * SCALE, box.minY * SCALE, box.minZ * SCALE,
                                    box.maxX * SCALE, box.maxY * SCALE, box.maxZ * SCALE
                            )
                    )
                    .toArray(VoxelShape[]::new);

            // faster than using union because we only do one simplify call at the end.
            shape = Arrays.stream(transformed).reduce(shape, (first, second) -> VoxelShapes.combine(first, second, BooleanBiFunction.OR));
        }

        return shape.simplify();
    }


}
