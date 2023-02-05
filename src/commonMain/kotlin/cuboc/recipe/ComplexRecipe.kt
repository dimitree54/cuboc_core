package cuboc.recipe

import cuboc.ingredient.Ingredient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import kotlin.math.min

open class ComplexRecipe(
    name: String,
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

class ParallelComplexRecipe private constructor(
    name: String,
    inputs: Set<RecipeInput>,
    outputs: Set<RecipeOutput>,
    instruction: Instruction,
    stages: List<Recipe>
) : ComplexRecipe(name, inputs, outputs, instruction, stages) {
    companion object {
        fun build(name: String, stages: List<Recipe>): ComplexRecipe {
            val requiredIngredients = mutableMapOf<Ingredient, Double>()
            val producedIngredients = mutableMapOf<Ingredient, Double>()
            var totalDuration = 0
            val instructionText = StringBuilder()
            stages.forEachIndexed { index, recipe ->
                for (input in recipe.inputs) {
                    val amountExisting = producedIngredients.getOrElse(input.ingredient) { 0.0 }
                    val amountToTake = min(amountExisting, input.amount)
                    val amountToRequest = input.amount - amountToTake

                    if (amountToRequest > 0) {
                        requiredIngredients[input.ingredient] =
                            requiredIngredients.getOrElse(input.ingredient) { 0.0 } + amountToRequest
                    }
                    if (amountToTake > 0) {
                        producedIngredients[input.ingredient] =
                            producedIngredients.getOrElse(input.ingredient) { 0.0 } - amountToTake
                    }
                }
                for (output in recipe.outputs) {
                    producedIngredients[output.ingredient] =
                        producedIngredients.getOrElse(output.ingredient) { 0.0 } + output.amount
                }
                totalDuration += recipe.instruction.durationMinutes
                instructionText.append("${index + 1}. ${recipe.instruction.text}\n")
            }
            return ComplexRecipe(
                name,
                requiredIngredients.map { RecipeInput(it.key, it.value, false) }.toSet(),
                producedIngredients.map { RecipeOutput(it.key, it.value, false) }.toSet(),
                Instruction(totalDuration, instructionText.toString()),
                stages
            )
        }
    }
}


class SequentialComplexRecipe private constructor(
    name: String,
    inputs: Set<RecipeInput>,
    outputs: Set<RecipeOutput>,
    instruction: Instruction,
    stages: List<Recipe>
) : ComplexRecipe(name, inputs, outputs, instruction, stages) {
    companion object {
        // todo ignore trivial recipes
        fun build(name: String, stages: List<Recipe>): ComplexRecipe {
            val requiredIngredients = mutableMapOf<Ingredient, Double>()
            val producedIngredients = mutableMapOf<Ingredient, Double>()
            var totalDuration = 0
            val instructionText = StringBuilder()
            for (recipe in stages) {
                for (input in recipe.inputs) {
                    val existingAmount = producedIngredients.getOrElse(input.ingredient) { 0.0 }
                    val takenAmount = min(existingAmount, input.amount)
                    val extraAmount = input.amount - takenAmount
                    if (takenAmount == existingAmount) {
                        producedIngredients.remove(input.ingredient)
                    } else {
                        producedIngredients[input.ingredient] = existingAmount - takenAmount
                    }
                    if (extraAmount > 0) {
                        requiredIngredients[input.ingredient] =
                            requiredIngredients.getOrElse(input.ingredient) { 0.0 } + extraAmount
                    }
                }
                for (output in recipe.outputs) {
                    producedIngredients[output.ingredient] =
                        producedIngredients.getOrElse(output.ingredient) { 0.0 } + output.amount
                }
                totalDuration += recipe.instruction.durationMinutes
            }
            return ComplexRecipe(
                name,
                requiredIngredients.map { RecipeInput(it.key, it.value, false) }.toSet(),
                producedIngredients.map { RecipeOutput(it.key, it.value, false) }.toSet(),
                Instruction(totalDuration, instructionText.toString()),
                stages
            )
        }
    }
}
