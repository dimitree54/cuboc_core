package cuboc.database

import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc.recipe.Scenario

interface CUBOCDatabase {
    val resources: ResourcesDatabase
    val recipes: RecipesDatabase

    fun execute(scenario: Scenario): PieceOfResource? {
        for (recipeInput in scenario.recipe.inputs) {
            val resourceRequests = scenario.resources[recipeInput]!!
            for (resourceRequest in resourceRequests) {
                require(resources.get(resourceRequest) != null)
            }
        }
        var requestedResource: Resource? = null
        for (recipeOutput in scenario.recipe.outputs) {
            val resource = resources.put(recipeOutput.ingredient, recipeOutput.amount)
                ?: throw IllegalArgumentException("Transaction failed, can not put resource")
            if (recipeOutput.ingredient == scenario.request.ingredient) {
                requestedResource = resource
            }
        }
        return requestedResource?.let { resources.get(PieceOfResource(it, scenario.request.amount)) }
    }
}