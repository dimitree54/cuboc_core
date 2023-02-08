package cuboc

import cuboc.database.CUBOCDatabase
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource
import cuboc.recipe.*
import cuboc_core.cuboc.database.search.RecipeSearchResult
import cuboc_core.cuboc.database.search.ResourceSearchResult
import cuboc_core.cuboc.database.search.SearchRequest
import cuboc_core.cuboc.database.search.SearchType
import utility.Name
import kotlin.math.ceil

class ScenariosBuilder(
    private val database: CUBOCDatabase,
    private val searchDepth: Int
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

    private suspend fun chooseBestResources(
        request: Resource,
        recipeInput: RecipeInput
    ): List<PieceOfResource>? {
        val resourceRequests = mutableListOf<PieceOfResource>()
        var amountLeft = recipeInput.amount
        val searchRequest = SearchRequest(recipeInput.ingredient.name.toString(), SearchType.Resources)
        for (searchResult in database.search(searchRequest)) {
            val userResource = (searchResult as ResourceSearchResult).resource
            if (userResource.resource.amount >= amountLeft) {
                resourceRequests.add(PieceOfResource(userResource, amountLeft))
                amountLeft = 0.0
                break
            } else {
                resourceRequests.add(PieceOfResource(userResource, userResource.resource.amount))
                amountLeft -= userResource.resource.amount
            }
        }
        return if (amountLeft > 0) null else resourceRequests
    }

    private suspend fun chooseBestResources(
        request: Resource,
        recipe: Recipe
    ): Map<RecipeInput, List<PieceOfResource>>? {
        val resourceRequests = mutableMapOf<RecipeInput, List<PieceOfResource>>()
        for (recipeInput in recipe.inputs) {
            resourceRequests[recipeInput] = chooseBestResources(request, recipeInput) ?: return null
        }
        return resourceRequests
    }

    private fun getCost(recipe: Recipe): Double {
        return recipe.instruction.durationMinutes.toDouble()
    }

    private fun getCost(resource: PieceOfResource): Double? {
        return 0.0
    }

    private fun getCost(scenario: Scenario): Double? {
        val recipeCost = getCost(scenario.recipe)
        val resourcesCost = scenario.resources.values.flatten().sumOf { getCost(it) ?: return null }
        return recipeCost + resourcesCost
    }

    private suspend fun getTrivialScenario(request: Resource): Scenario? {
        val trivialRecipe = TrivialRecipe(request)
        val resources = chooseBestResources(request, trivialRecipe) ?: return null
        return Scenario(request, trivialRecipe, resources)
    }

    private fun combine(inputRecipes: List<Recipe>, outputRecipe: Recipe): Recipe {
        val inputsRecipe = ParallelComplexRecipe.build(Name("inputs"), inputRecipes)
        return SequentialComplexRecipe.build(Name("recipe"), listOf(inputsRecipe, outputRecipe))
    }

    suspend fun searchForBestScenario(
        request: Resource, maxDepth: Int = searchDepth, maxCost: Double = Double.MAX_VALUE
    ): Scenario? {
        val trivialScenario = getTrivialScenario(request)
        val trivialScenarioCost = trivialScenario?.let { getCost(it) }
        var bestScenario: Scenario? = trivialScenario
        var bestCost = trivialScenarioCost ?: maxCost
        if (maxDepth == 0) {
            return bestScenario
        }
        val searchRequest = SearchRequest(request.ingredient.name.toString(), SearchType.RecipesByOutput)
        for (searchResult in database.search(searchRequest)) {
            val recipe = (searchResult as RecipeSearchResult).recipe
            val scaledRecipe = scaleRecipe(request, recipe) ?: continue
            val strippedCost = getCost(scaledRecipe)
            if (strippedCost >= bestCost) {
                continue
            }

            val inputRecipes = mutableListOf<Recipe>()
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
                inputRecipes.add(inputScenario.recipe)
                maxInputCost -= inputScenarioCost
            }

            if (recipePossible) {
                val combinedRecipe = combine(inputRecipes, scaledRecipe)
                val resources = chooseBestResources(request, combinedRecipe) ?: continue
                val scenario = Scenario(request, combinedRecipe, resources)
                val scenarioCost = getCost(scenario) ?: continue
                if (scenarioCost < bestCost) {
                    bestScenario = scenario
                    bestCost = scenarioCost
                }
            }
        }
        return bestScenario
    }
}
