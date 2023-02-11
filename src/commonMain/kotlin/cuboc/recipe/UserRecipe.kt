package cuboc.recipe

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import kotlinx.serialization.Serializable

@Serializable
data class UserRecipe(
    val id: String,
    val recipe: Recipe
) {
    val inputs: Set<RecipeInput>
        get() = recipe.inputs

    val outputs: Set<RecipeOutput>
        get() = recipe.outputs
}