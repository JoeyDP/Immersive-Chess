package be.immersivechess.block.entity;

import be.immersivechess.ImmersiveChess;
import be.immersivechess.block.Blocks;
import com.mojang.datafixers.types.Type;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class BlockEntityTypes {

    // Entity types
    public static BlockEntityType<PieceBlockEntity> PIECE_BLOCK_ENTITY_TYPE = create("piece_block_entity", FabricBlockEntityTypeBuilder.create(PieceBlockEntity::new).addBlocks(Blocks.PIECES.values().toArray(new Block[0])));
    public static BlockEntityType<PieceStandBlockEntity> PIECE_STAND_BLOCK_ENTITY_TYPE = create("piece_stand_block_entity", FabricBlockEntityTypeBuilder.create(PieceStandBlockEntity::new).addBlocks(Blocks.PIECE_STANDS.toArray(new Block[0])));

    public static BlockEntityType<BoardBlockEntity> BOARD_BLOCK_ENTITY_TYPE = create("board_block_entity", FabricBlockEntityTypeBuilder.create(BoardBlockEntity::new).addBlocks(Blocks.BOARD_BLOCK));
    public static BlockEntityType<PieceStructureBlockEntity> PIECE_STRUCTURE_BLOCK_ENTITY_TYPE = create("piece_structure_block_entity", FabricBlockEntityTypeBuilder.create(PieceStructureBlockEntity::new).addBlocks(Blocks.PIECE_STRUCTURE_BLOCKS.toArray(new Block[0])));


    public static void onInitialize(){
    }

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, FabricBlockEntityTypeBuilder<T> builder) {
        Type<?> type = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(ImmersiveChess.MOD_ID, id), builder.build(type));
    }


}
