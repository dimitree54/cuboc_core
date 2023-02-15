package cuboc.scenario

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioInProgress(
    val id: String,
    val request: List<Resource>,
    val scenario: Scenario,
    val externalResources: Map<ScenarioStage, List<PieceOfUserResource>>,
    val stagesInProgress: Map<ScenarioStage, ScenarioStageInProgress> = emptyMap()
) {
    val finalStage: ScenarioStage
    val extendedScenario: Scenario

    init {
        require(request.isNotEmpty()) { "Empty request" }
        finalStage = ScenarioStage.buildTrivialStage(id, request)
        extendedScenario = scenario.addSequentialStage(finalStage)
        require(finalStage !in extendedScenario.externalResourcesRequired) { "Scenario does not fulfill the request" }
        for ((stageRequiring, resourcesRequired) in extendedScenario.externalResourcesRequired) {
            val resourcesProvided = externalResources[stageRequiring]
                ?: throw IllegalArgumentException("External input resource not provided for some stages")
            require(resourcesRequired.size == resourcesProvided.size) { "Some requested resources not provided" }
            for ((resourceRequired, resourceProvided) in resourcesRequired.zip(resourcesProvided)) {
                require(resourceRequired.ingredient == resourceProvided.resource.ingredient) { "Invalid order of provided resources" }
                require(resourceRequired.amount == resourceProvided.amount) { "Invalid amount provided" }
            }
        }
        for ((stage, stageInProgress) in stagesInProgress) {
            require(stage in extendedScenario.stages) { "Unknown stage in progress" }
            require(stage.id == stageInProgress.id) { "Bad stages in progress mapping" }
        }
    }
}