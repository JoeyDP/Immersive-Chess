package be.immersivechess.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;

public class Recipes {

    public static final RecipeSerializer<StandDyeRecipe> STAND_DYE = RecipeSerializer.register("crafting_special_standdye", new SpecialRecipeSerializer<>(StandDyeRecipe::new));


    public static void onInitialize(){

    }

}
