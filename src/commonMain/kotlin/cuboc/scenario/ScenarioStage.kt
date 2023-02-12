package cuboc.scenario

import cuboc.ingredient.Resource
import cuboc.recipe.Recipe
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioStage(
    val id: String,
    val recipe: Recipe,
) {
    fun calcStageResourcesInfo(providedResources: List<Resource>): StageResourcesInfo {
        val notUsedResources = providedResources.toMutableList()
        val extraRequiredResources = mutableListOf<Resource>()
        for (recipeInput in recipe.inputs) {
            var amountLeftToFulfill = recipeInput.amount
            val relevantIndexes = notUsedResources.mapIndexed { index, resource ->
                if (resource.ingredient == recipeInput.ingredient) index else null
            }.filterNotNull()
            for (relevantIndex in relevantIndexes) {
                val relevantResource = notUsedResources[relevantIndex]
                val amountAvailable = relevantResource.amount
                val amountToTake = minOf(amountLeftToFulfill, amountAvailable)
                val amountLeft = amountAvailable - amountToTake
                amountLeftToFulfill -= amountToTake
                notUsedResources[relevantIndex] = Resource(relevantResource.ingredient, amountLeft)
            }
            if (amountLeftToFulfill > 0) {
                extraRequiredResources.add(Resource(recipeInput.ingredient, amountLeftToFulfill))
            }
        }
        val outputResourcesAfterStage = recipe.outputs.map { Resource(it.ingredient, it.amount) }
        return StageResourcesInfo(
            providedResources = providedResources,
            extraRequiredResources = extraRequiredResources,
            notUsedResources = notUsedResources,
            producedResources = outputResourcesAfterStage
        )
    }
}