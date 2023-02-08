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
    override fun getScaled(scaleFactor: Double): ComplexRecipe {
        return ComplexRecipe(
            name,
            inputs.map { it.getScaled(scaleFactor) }.toSet(),
            outputs.map { it.getScaled(scaleFactor) }.toSet(),
            instruction.getScaled(scaleFactor),
            stages.map { it.getScaled(scaleFactor) }
        )
    }
}
