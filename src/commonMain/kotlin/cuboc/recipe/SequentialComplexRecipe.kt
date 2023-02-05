package cuboc.recipe

import cuboc.ingredient.Ingredient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import kotlin.math.min

class SequentialComplexRecipe private constructor(
    name: String,
    inputs: Set<RecipeInput>,
    outputs: Set<RecipeOutput>,
    instruction: Instruction,
    stages: List<Recipe>
) : ComplexRecipe(name, inputs, outputs, instruction, stages) {
    companion object {
        fun build(name: String, stages: List<Recipe>): SequentialComplexRecipe {
            val requiredIngredients = mutableMapOf<Ingredient, Double>()
            val producedIngredients = mutableMapOf<Ingredient, Double>()
            var totalDuration = 0
            val instructionText = StringBuilder()
            stages.forEachIndexed { index, recipe ->
                for (input in recipe.inputs) {
                    val existingAmount = producedIngredients.getOrElse(input.ingredient) { 0.0 }
                    val takenAmount = min(existingAmount, input.amount)
                    val extraRequiredAmount = input.amount - takenAmount
                    if (takenAmount == existingAmount) {
                        producedIngredients.remove(input.ingredient)
                    } else {
                        producedIngredients[input.ingredient] = existingAmount - takenAmount
                    }
                    if (extraRequiredAmount > 0) {
                        requiredIngredients[input.ingredient] =
                            requiredIngredients.getOrElse(input.ingredient) { 0.0 } + extraRequiredAmount
                    }
                }
                for (output in recipe.outputs) {
                    producedIngredients[output.ingredient] =
                        producedIngredients.getOrElse(output.ingredient) { 0.0 } + output.amount
                }
                totalDuration += recipe.instruction.durationMinutes
                instructionText.append("${index + 1}. ${recipe.instruction.text}\n")
            }
            return SequentialComplexRecipe(
                name,
                requiredIngredients.map { RecipeInput(it.key, it.value, false) }.toSet(),
                producedIngredients.map { RecipeOutput(it.key, it.value, false) }.toSet(),
                Instruction(totalDuration, instructionText.toString()),
                stages
            )
        }
    }
}