package cuboc.scenario

import cuboc.database.CUBOCDatabase
import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import cuboc.ingredient.Resource
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import cuboc_core.cuboc.database.search.RecipeSearchResult
import cuboc_core.cuboc.database.search.ResourceSearchResult
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchType
import cuboc_core.utility.IdGenerator
import utility.Name
import utility.Text
import kotlin.math.ceil

class ScenarioBuilder(
    private val database: CUBOCDatabase,
    private val searchDepth: Int
) {
    private val scenarioStagesIdGenerator = IdGenerator()

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

    private suspend fun chooseBestUserResources(
        resource: Resource
    ): List<PieceOfUserResource>? {
        val resourceRequests = mutableListOf<PieceOfUserResource>()
        var amountLeft = resource.amount
        val searchRequest = SearchRequest(resource.ingredient.name.toString(), SearchType.Resources)
        for (searchResult in database.search(searchRequest)) {
            val userResource = (searchResult as ResourceSearchResult).resource
            if (userResource.amount >= amountLeft) {
                resourceRequests.add(PieceOfUserResource(userResource, amountLeft))
                amountLeft = 0.0
                break
            } else {
                resourceRequests.add(PieceOfUserResource(userResource, userResource.amount))
                amountLeft -= userResource.amount
            }
        }
        return if (amountLeft > 0) null else resourceRequests
    }

    private suspend fun chooseBestUserResources(
        recipe: Recipe
    ): Map<RecipeInput, List<PieceOfUserResource>>? {
        val resourceRequests = mutableMapOf<RecipeInput, List<PieceOfUserResource>>()
        for (recipeInput in recipe.inputs) {
            resourceRequests[recipeInput] = chooseBestUserResources(recipeInput.resource) ?: return null
        }
        return resourceRequests
    }

    private fun getCost(recipe: Recipe): Double {
        return recipe.instruction.durationMinutes.toDouble()
    }

    private fun getCost(pieceOfUserResource: PieceOfUserResource): Double? {
        return 0.0
    }

    private suspend fun getCost(resource: Resource): Double? {
        return chooseBestUserResources(resource)?.sumOf { getCost(it) ?: return null } ?: return null
    }

    private suspend fun getCost(scenario: Scenario): Double? {
        val recipeCost = scenario.stages.sumOf { getCost(it.recipe) }
        val resourcesCost = scenario.externalResourcesRequired.values.sumOf { getCost(it) ?: return null }
        return recipeCost + resourcesCost
    }

    private fun getTrivialScenario(request: Resource): Scenario {
        val trivialRecipe = Recipe(
            Name("Trivial recipe of ${request.ingredient.name}"),
            setOf(RecipeInput(request.ingredient, request.amount, true)),
            setOf(RecipeOutput(request.ingredient, request.amount, true)),
            Instruction(0, Text("Just take ${request.amount} of ${request.ingredient.name}"))
        )
        return Scenario(
            setOf(ScenarioStage(scenarioStagesIdGenerator.generateId(), trivialRecipe)),
            mapOf()
        )
    }

    private fun combine(inputScenarios: List<Scenario>, outputRecipe: Recipe): Scenario {
        var inputScenario = inputScenarios.first()
        for (i in 1 until inputScenarios.size) {
            inputScenario = inputScenario.joinParallel(inputScenarios[i])
        }
        return inputScenario.addSequentialStage(ScenarioStage(scenarioStagesIdGenerator.generateId(), outputRecipe))
    }

    suspend fun searchForBestScenario(
        request: Resource, maxDepth: Int = searchDepth, maxCost: Double = Double.MAX_VALUE
    ): Scenario? {
        val trivialScenario = getTrivialScenario(request)
        val trivialScenarioCost = getCost(trivialScenario)
        var bestScenario: Scenario? = trivialScenario
        var bestCost = trivialScenarioCost ?: maxCost
        if (maxDepth == 0) {
            return bestScenario
        }
        val searchRequest = SearchRequest(request.ingredient.name.toString(), SearchType.RecipesByOutput)
        for (searchResult in database.search(searchRequest)) {
            val recipe = (searchResult as RecipeSearchResult).recipe.recipe
            val scaledRecipe = scaleRecipe(request, recipe) ?: continue
            val strippedCost = getCost(scaledRecipe)
            if (strippedCost >= bestCost) {
                continue
            }

            val inputScenarios = mutableListOf<Scenario>()
            var maxInputCost = bestCost - strippedCost
            var recipePossible = true
            for (recipeInput in scaledRecipe.inputs) {
                val inputScenario = searchForBestScenario(
                    Resource(recipeInput.ingredient, recipeInput.amount), maxDepth - 1, maxCost
                )
                val inputScenarioCost = inputScenario?.let { getCost(it) }
                if (inputScenarioCost == null) {
                    recipePossible = false
                    break
                }
                inputScenarios.add(inputScenario)
                maxInputCost -= inputScenarioCost
            }

            if (recipePossible) {
                val combinedScenario = combine(inputScenarios, scaledRecipe)
                val scenarioCost = getCost(combinedScenario) ?: continue
                if (scenarioCost < bestCost) {
                    bestScenario = combinedScenario
                    bestCost = scenarioCost
                }
            }
        }
        return bestScenario
    }
}
