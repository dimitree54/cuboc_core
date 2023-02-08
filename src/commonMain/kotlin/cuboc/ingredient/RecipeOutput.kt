package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class RecipeOutput(val ingredient: Ingredient, val amount: Double, val scalable: Boolean) {
    fun getScaled(scaleFactor: Double): RecipeOutput {
        return RecipeOutput(ingredient, amount * scaleFactor, scalable)
    }
}