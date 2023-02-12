package cuboc.scenario

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioInProgress(
    val id: String,
    val request: List<Resource>,
    val scenario: Scenario,
    val externalResources: Map<ScenarioStage, List<PieceOfUserResource>>
) {
    init {
        for ((stageRequiring, resourcesRequired) in scenario.externalResourcesRequired) {
            val resourcesProvided = externalResources[stageRequiring]
                ?: throw IllegalArgumentException("External input resource not provided for some stages")
            require(resourcesRequired.size == resourcesProvided.size) { "Some requested resources not provided" }
            for ((resourceRequired, resourceProvided) in resourcesRequired.zip(resourcesProvided)) {
                require(resourceRequired.ingredient == resourceProvided.resource.ingredient) { "Invalid order of provided resources" }
                require(resourceRequired.amount == resourceProvided.amount) { "Invalid amount provided" }
            }
        }
        // check request fulfilled
        scenario.producedResources.find { it.ingredient == request.ingredient }?.let {
            require(it.amount >= request.amount) { "Not enough of requested resources produced" }
        } ?: throw IllegalArgumentException("Request ingredient not produced")
    }
}