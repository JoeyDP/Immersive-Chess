package be.immersivechess;

import be.immersivechess.advancement.criterion.Criteria;
import be.immersivechess.block.Blocks;
import be.immersivechess.block.entity.BlockEntityTypes;
import be.immersivechess.item.Items;
import be.immersivechess.recipe.Recipes;
import be.immersivechess.resource.BlockStateLuminanceMapper;
import be.immersivechess.screen.ScreenHandlerTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ImmersiveChess implements ModInitializer {
	public static final String MOD_ID = "immersivechess";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(BlockStateLuminanceMapper.INSTANCE);

		Blocks.onInitialize();
		BlockEntityTypes.onInitialize();

		Items.onInitialize();
		Recipes.onInitialize();

		Criteria.onInitialize();

		ScreenHandlerTypes.onInitialize();
	}
}
