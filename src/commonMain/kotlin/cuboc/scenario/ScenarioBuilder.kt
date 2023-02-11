package cuboc.scenario

import cuboc.database.CUBOCDatabaseClient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import cuboc.ingredient.Resource
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import cuboc_core.cuboc.database.search.RecipeSearchResult
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchType
import cuboc_core.utility.IdGenerator
import utility.Name
import utility.Text
import kotlin.math.ceil

class ScenarioBuilder(
    private val database: CUBOCDatabaseClient,
    private val searchDepth: Int,
    private val scenarioStagesIdGenerator: IdGenerator
) {
    private fun scaleRecipe(request: Resource, recipe: Recipe): Recipe? {
        val recipeOutput = recipe.outputs.find { it.ingredient == request.ingredient } ?: return null
        val scaleFactor = if (request.amount > recipeOutput.amount) {
            if (!recipeOutput.scalable) {
                return null
            }
            ceil(request.amount / recipeOutput.amount)
        } else {
            1.0
        }
        return recipe.getScaled(scaleFactor)
    }

    private fun getTrivialScenario(request: Resource): Scenario {
        val trivialRecipe = Recipe(
            Name("Trivial recipe of ${request.ingredient.name}"),
            setOf(RecipeInput(request.ingredient, request.amount, true)),
            setOf(RecipeOutput(request.ingredient, request.amount, true)),
            Instruction(0, Text("Just take ${request.amount} of ${request.ingredient.name}"))
        )
        return Scenario(
            setOf(
                ScenarioStage(
                    scenarioStagesIdGenerator.generateId(request.ingredient.name.toString()),
                    trivialRecipe
                )
            ),
            mapOf()
        )
    }

    private fun combine(inputScenarios: List<Scenario>, outputRecipe: Recipe): Scenario {
        var inputScenario = inputScenarios.first()
        for (i in 1 until inputScenarios.size) {
            inputScenario = inputScenario.joinParallel(inputScenarios[i])
        }
        return inputScenario.addSequentialStage(
            ScenarioStage(
                scenarioStagesIdGenerator.generateId(outputRecipe.name.toString()),
                outputRecipe
            )
        )
    }

    suspend fun searchForBestScenario(
        request: Resource, maxDepth: Int = searchDepth, maxCost: Double = Double.MAX_VALUE
    ): Scenario? {
        val trivialScenario = getTrivialScenario(request)
        val trivialScenarioCost = database.getCost(trivialScenario)
        var bestScenario: Scenario? = trivialScenario
        var bestCost = trivialScenarioCost ?: maxCost
        if (maxDepth == 0) {
            return bestScenario
        }
        val searchRequest = SearchRequest(request.ingredient.name.toString(), SearchType.RecipesByOutput)
        for (searchResult in database.search(searchRequest)) {
            val recipe = (searchResult as RecipeSearchResult).userRecipe.recipe
            val scaledRecipe = scaleRecipe(request, recipe) ?: continue

            val inputScenarios = mutableListOf<Scenario>()
            var maxInputCost = bestCost
            var recipePossible = true
            for (recipeInput in scaledRecipe.inputs) {
                val inputScenario = searchForBestScenario(
                    Resource(recipeInput.ingredient, recipeInput.amount), maxDepth - 1, maxCost
                )
                val inputScenarioCost = inputScenario?.let { database.getCost(it) }
                if (inputScenarioCost == null) {
                    recipePossible = false
                    break
                }
                inputScenarios.add(inputScenario)
                maxInputCost -= inputScenarioCost
            }

            if (recipePossible) {
                val combinedScenario = combine(inputScenarios, scaledRecipe)
                val scenarioCost = database.getCost(combinedScenario) ?: continue
                if (scenarioCost < bestCost) {
                    bestScenario = combinedScenario
                    bestCost = scenarioCost
                }
            }
        }
        return bestScenario
    }
}
