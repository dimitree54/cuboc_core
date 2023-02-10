package cuboc.recipe

import kotlinx.serialization.Serializable

@Serializable
data class UserRecipe(
    val id: String,
    val recipe: Recipe
)