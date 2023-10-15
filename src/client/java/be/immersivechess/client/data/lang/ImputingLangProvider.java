package be.immersivechess.client.data.lang;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ImputingLangProvider extends BaseLangProvider {
    protected ImputingLangProvider(FabricDataOutput dataOutput, String languageCode) {
        super(dataOutput, languageCode);
    }

    public static FabricDataGenerator.Pack.Factory<ImputingLangProvider> factory(String languageCode) {
        return output -> new ImputingLangProvider(output, languageCode);
    }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        try {
            translationBuilder.add(getStaticLangFilePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        translationBuilder = getFillMissingTranslationBuilder(translationBuilder);

        // Fill in missing from registries
        addIfMissing(Registries.ITEM, translationBuilder::add);
        addIfMissing(Registries.BLOCK, translationBuilder::add);
        addIfMissing(Registries.ENTITY_TYPE, translationBuilder::add);
        addIfMissing(Registries.ENCHANTMENT, translationBuilder::add);
        addIfMissing(Registries.ATTRIBUTE, translationBuilder::add);
        addIfMissing(Registries.STAT_TYPE, translationBuilder::add);
        addIfMissing(Registries.STATUS_EFFECT, translationBuilder::add);

        // a lot of work because out of all registries, itemgroups are the only one that use keys iso the actual value
        // because it needs to be final apparently
        TranslationBuilder tb = translationBuilder;
        addIfMissing(Registries.ITEM_GROUP, (ig, val) -> tb.add(Registries.ITEM_GROUP.getKey(ig).get(), val));
    }

    private <T> void addIfMissing(Registry<T> registry, BiConsumer<T, String> consumer) {
        for (Map.Entry<? extends RegistryKey<T>, T> entry : registry.getEntrySet()) {
            if (withinNamespace(entry.getKey().getValue())) {
                consumer.accept(entry.getValue(), entry.getKey().getValue().getPath());
            }
        }
    }

    private boolean withinNamespace(Identifier id){
        return id.getNamespace().equals(dataOutput.getModId());
    }
}
