package cuboc.scenario

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.RecipeInput
import cuboc.recipe.Recipe
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioStageInProgress(
    val id: String,
    val stage: ScenarioStage,
    val resourcesReservedForStage: List<PieceOfUserResource>,
    val state: ScenarioStageStatus
) {
    val inputResources: Map<RecipeInput, List<PieceOfUserResource>>
    val extraResourcesReserved: List<PieceOfUserResource>

    init {
        val allInputResources = mutableMapOf<RecipeInput, List<PieceOfUserResource>>()
        val occupiedResources = mutableMapOf<PieceOfUserResource, Double>()
        for (recipeInput in stage.recipe.inputs) {
            val resourcesForRecipeInput = mutableListOf<PieceOfUserResource>()
            var amountRequired = recipeInput.amount
            for (reservedResource in resourcesReservedForStage.filter { it.resource.ingredient == recipeInput.ingredient }) {
                val amountAvailable =
                    occupiedResources[reservedResource]?.let { reservedResource.amount - it } ?: reservedResource.amount
                val amountTaken = minOf(amountRequired, amountAvailable)
                amountRequired -= amountTaken

                occupiedResources[reservedResource] = (occupiedResources[reservedResource] ?: 0.0) + amountTaken
                resourcesForRecipeInput.add(PieceOfUserResource(reservedResource.userResource, amountTaken))

                if (amountRequired == 0.0) {
                    break
                }
            }
            if (amountRequired > 0.0) {
                throw IllegalArgumentException("Not enough resources reserved for stage")
            }
            allInputResources[recipeInput] = resourcesForRecipeInput
        }
        inputResources = allInputResources

        val extraResourcesReserved = mutableListOf<PieceOfUserResource>()
        for (reservedResource in resourcesReservedForStage) {
            val amountTaken = occupiedResources[reservedResource] ?: 0.0
            if (amountTaken < reservedResource.amount) {
                extraResourcesReserved.add(
                    PieceOfUserResource(
                        reservedResource.userResource,
                        reservedResource.amount - amountTaken
                    )
                )
            }
        }
        this.extraResourcesReserved = extraResourcesReserved
    }

    fun getWithUpdatedState(newState: ScenarioStageStatus): ScenarioStageInProgress {
        return copy(state = newState)
    }

    val recipe: Recipe
        get() = stage.recipe
}