package cuboc.recipe

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import kotlinx.serialization.Serializable
import utility.Name

@Serializable
data class Recipe(
    val name: Name,
    val inputs: Set<RecipeInput>,
    val outputs: Set<RecipeOutput>,
    val instruction: Instruction
) {
    fun getScaled(scaleFactor: Double): Recipe {
        return Recipe(
            name,
            inputs.map { it.getScaled(scaleFactor) }.toSet(),
            outputs.map { it.getScaled(scaleFactor) }.toSet(),
            instruction.getScaled(scaleFactor)
        )
    }
}