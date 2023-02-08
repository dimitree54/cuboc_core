package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class RecipeInput(val ingredient: Ingredient, val amount: Double, val scalable: Boolean) {
    fun getScaled(scaleFactor: Double): RecipeInput {
        return RecipeInput(ingredient, amount * scaleFactor, scalable)
    }
}