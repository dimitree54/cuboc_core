package cuboc.recipe

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import utility.Name

open class ComplexRecipe(
    name: Name,
    inputs: Set<RecipeInput>,
    outputs: Set<RecipeOutput>,
    instruction: Instruction,
    val stages: List<Recipe>
) : Recipe(name, inputs, outputs, instruction) {
    override fun scale(scaleFactor: Double) {
        super.scale(scaleFactor)
        stages.forEach { it.scale(scaleFactor) }
    }
}
