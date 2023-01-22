package cuboc.recipe

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput

open class Recipe(
    val name: String, val inputs: Set<RecipeInput>, val outputs: Set<RecipeOutput>, val instruction: Instruction
) {
    open fun scale(scaleFactor: Double) {
        inputs.forEach { it.scale(scaleFactor) }
        outputs.forEach { it.scale(scaleFactor) }
        instruction.scale(scaleFactor)
    }
}

