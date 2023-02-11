package cuboc.scenario

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioInProgress(
    val id: String,
    val request: Resource,
    val scenario: Scenario,
    val externalResources: Map<RecipeInput, List<PieceOfUserResource>>
) {
    init {
        // check enough input resources
        for (inputResource in scenario.externalResourcesRequired) {
            val userResources = externalResources[inputResource.key]
                ?: throw IllegalArgumentException("External input resource not provided")
            require(userResources.sumOf { it.amount } >= inputResource.value.amount) { "Not enough external resources provided" }
        }
        // check request fulfilled
        scenario.producedResources.find { it.ingredient == request.ingredient }?.let {
            require(it.amount >= request.amount) { "Not enough of requested resources produced" }
        } ?: throw IllegalArgumentException("Request ingredient not produced")
    }
}