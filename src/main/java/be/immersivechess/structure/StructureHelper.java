package be.immersivechess.structure;

import be.immersivechess.mixin.MixinStructureAccessor;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StructureHelper {

    public static List<StructureTemplate.StructureBlockInfo> getBlockInfoList(StructureTemplate structure){
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((MixinStructureAccessor) structure).getBlockInfoLists();
        if (blockInfoLists.isEmpty()){
            return new ArrayList<>();
        }else if (blockInfoLists.size() > 1){
            throw new IllegalStateException("getBlockInfoList() called on a structure with multiple palettes");
        }
        return blockInfoLists.get(0).getAll();
    }

    public static Map<BlockPos, BlockState> buildBlockStateMap(StructureTemplate structure){
        List<StructureTemplate.StructureBlockInfo> blockInfoList = getBlockInfoList(structure);
        return blockInfoList.stream().collect(Collectors.toMap(s -> s.pos, s -> s.state));
    }

    public static StructureTemplate rotate(StructureTemplate structure, BlockRotation rotation){
        // extract data from structureTemplate
        List<StructureTemplate.StructureBlockInfo> blockInfos = getBlockInfoList(structure);

        // rotation
        StructurePlacementData placementData = new StructurePlacementData().setRotation(rotation);
        BlockPos offset = structure.offsetByTransformedSize(BlockPos.ORIGIN, BlockMirror.NONE, rotation);
        List<StructureTemplate.StructureBlockInfo> blockInfos2 = Lists.newArrayList();
        for (StructureTemplate.StructureBlockInfo structureBlockInfo : blockInfos) {
            BlockPos blockPos = StructureTemplate.transform(placementData, structureBlockInfo.pos).add(offset);
            BlockState blockState = structureBlockInfo.state.rotate(rotation);
            StructureTemplate.StructureBlockInfo structureBlockInfo2 = new StructureTemplate.StructureBlockInfo(blockPos, blockState, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.copy() : null);
            blockInfos2.add(structureBlockInfo2);
        }

        // write list back to StructureTemplate
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = ((MixinStructureAccessor) structure).getBlockInfoLists();
        blockInfoLists.clear();
        blockInfoLists.add(new StructureTemplate.PalettedBlockInfoList(blockInfos2));

        return structure;
    }

}
