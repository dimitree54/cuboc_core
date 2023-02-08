package cuboc.recipe

import cuboc.ingredient.Ingredient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import utility.Text

class ParallelComplexRecipe private constructor(
    name: String,
    inputs: Set<RecipeInput>,
    outputs: Set<RecipeOutput>,
    instruction: Instruction,
    stages: List<Recipe>
) : ComplexRecipe(name, inputs, outputs, instruction, stages) {
    companion object {
        fun build(name: String, stages: List<Recipe>): ParallelComplexRecipe {
            val requiredIngredients = mutableMapOf<Ingredient, Double>()
            val producedIngredients = mutableMapOf<Ingredient, Double>()
            var totalDuration = 0
            val instructionText = StringBuilder()
            stages.forEachIndexed { index, recipe ->
                for (input in recipe.inputs) {
                    requiredIngredients[input.ingredient] =
                        requiredIngredients.getOrElse(input.ingredient) { 0.0 } + input.amount
                }
                for (output in recipe.outputs) {
                    producedIngredients[output.ingredient] =
                        producedIngredients.getOrElse(output.ingredient) { 0.0 } + output.amount
                }
                totalDuration += recipe.instruction.durationMinutes
                instructionText.append("${index + 1}. ${recipe.instruction.text.text}\n")
            }
            return ParallelComplexRecipe(
                name,
                requiredIngredients.map { RecipeInput(it.key, it.value, false) }.toSet(),
                producedIngredients.map { RecipeOutput(it.key, it.value, false) }.toSet(),
                Instruction(totalDuration, Text(instructionText.toString())),
                stages
            )
        }
    }
}