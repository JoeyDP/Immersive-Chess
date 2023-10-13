package be.immersivechess.client.data;

import be.immersivechess.client.data.lang.ExtendingLangProvider;
import be.immersivechess.client.data.lang.ImputingLangProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

@Environment(EnvType.CLIENT)
public class DataGenerator implements DataGeneratorEntrypoint {

    private static final String EN = "en_us";
    private static final String BE = "nl_be";
    private static final String NL = "nl_nl";

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // Languages, note: order is important!
        // English (default) extends the en_us static file with placeholders for missing values based on code. Logs warnings for all missing translations.
        pack.addProvider(ImputingLangProvider.factory(EN));
        // Flemish extends the nl_be static file with defaults from generated en_us file. Logs warnings for all missing translations.
        pack.addProvider(ExtendingLangProvider.factory(BE, EN));
        // Dutch extends the nl_nl static file with defaults from generated nl_be file. Does not log warnings for missing values (intentional).
        pack.addProvider(ExtendingLangProvider.factory(NL, BE, false));

        // colors to determine white/black
        pack.addProvider(BlockColorGenerator::new);
    }

}
