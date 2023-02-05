package cuboc.recipe

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput

class RecipePrototype(
    val name: String,
    val inputs: Set<RecipeInput>,
    val outputs: Set<RecipeOutput>,
    val instruction: Instruction
) {
    fun toRecipe(id: String): Recipe = Recipe(id, name, inputs, outputs, instruction)
}