package cuboc

import cuboc.ingredient.IngredientRequest
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.RecipeInput
import cuboc.database.RecipesDatabase
import cuboc.database.ResourcesDatabase
import cuboc.recipe.ComplexRecipe
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc.recipe.TrivialRecipe
import kotlin.math.ceil

class ScenariosBuilder(
    private val recipesDatabase: RecipesDatabase,
    private val resourcesDatabase: ResourcesDatabase,
    private val searchDepth: Int
) {
    private fun scaleRecipe(request: IngredientRequest, recipe: Recipe): Recipe? {
        val recipeOutput = recipe.outputs.find { it.ingredient == request.ingredient } ?: return null
        val scaleFactor = if (request.amount > recipeOutput.amount) {
            if (!recipeOutput.scalable) {
                return null
            }
            ceil(request.amount / recipeOutput.amount)
        } else {
            1.0
        }
        recipe.scale(scaleFactor)
        return recipe
    }

    private fun chooseBestResources(request: IngredientRequest, recipeInput: RecipeInput): List<PieceOfResource>? {
        val resourceRequests = mutableListOf<PieceOfResource>()
        var amountLeft = recipeInput.amount
        for (resource in resourcesDatabase.search(recipeInput.ingredient)) {
            if (resource.amount >= amountLeft) {
                resourceRequests.add(PieceOfResource(resource, amountLeft))
                amountLeft = 0.0
                break
            } else {
                resourceRequests.add(PieceOfResource(resource, resource.amount))
                amountLeft -= resource.amount
            }
        }
        return if (amountLeft > 0) null else resourceRequests
    }

    private fun chooseBestResources(request: IngredientRequest, recipe: Recipe): Map<RecipeInput, List<PieceOfResource>>? {
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

    private fun getTrivialScenario(request: IngredientRequest): Scenario? {
        val trivialRecipe = TrivialRecipe(request)
        val resources = chooseBestResources(request, trivialRecipe) ?: return null
        return Scenario(request, trivialRecipe, resources)
    }

    private fun combine(inputRecipes: List<Recipe>, outputRecipe: Recipe): Recipe {
        val inputsRecipe = ComplexRecipe.buildParallel("inputs", inputRecipes)
        return ComplexRecipe.buildSequential("recipe", listOf(inputsRecipe, outputRecipe))
    }

    fun searchForBestScenario(
        request: IngredientRequest, maxDepth: Int = searchDepth, maxCost: Double = Double.MAX_VALUE
    ): Scenario? {
        val trivialScenario = getTrivialScenario(request)
        val trivialScenarioCost = trivialScenario?.let { getCost(it) }
        var bestScenario: Scenario? = trivialScenario
        var bestCost = trivialScenarioCost ?: maxCost
        if (maxDepth == 0) {
            return bestScenario
        }
        for (recipe in recipesDatabase.search(listOf(request.ingredient))) {
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
                    IngredientRequest(recipeInput.ingredient, recipeInput.amount), maxDepth - 1, maxCost
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
