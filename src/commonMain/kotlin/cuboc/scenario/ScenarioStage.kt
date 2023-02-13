package cuboc.scenario

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import cuboc.ingredient.Resource
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import kotlinx.serialization.Serializable
import utility.Name
import utility.Text

@Serializable
data class ScenarioStage(
    val id: String,
    val recipe: Recipe,
) {
    val output = recipe.outputs.map { Resource(it.ingredient, it.amount) }

    companion object {
        fun buildTrivialStage(id: String, request: List<Resource>): ScenarioStage {
            val trivialRecipe = Recipe(
                Name("Trivial recipe"),
                request.map { RecipeInput(it.ingredient, it.amount, true) }.toSet(),
                request.map { RecipeOutput(it.ingredient, it.amount, true) }.toSet(),
                Instruction(0, Text("Just take it"))
            )
            return ScenarioStage(
                id,
                trivialRecipe
            )
        }
    }
}