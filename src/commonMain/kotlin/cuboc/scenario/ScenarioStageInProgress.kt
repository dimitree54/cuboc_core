package cuboc.scenario

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.UserResource
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioStageInProgress(
    val id: String,
    val stage: ScenarioStage,
    val inputResources: Map<RecipeInput, UserResource>,
    val state: ScenarioStageStatus
) {
    init {
        // check enough input resources
        for (inputResource in stage.recipe.inputs) {
            val userResource =
                inputResources[inputResource] ?: throw IllegalArgumentException("Input resource not provided")
            require(userResource.resource.amount >= inputResource.amount) { "Not enough resources provided" }
        }
    }
}