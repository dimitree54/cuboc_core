package cuboc.scenario

import cuboc.ingredient.Resource

data class StageResourcesInfo(
    val providedResources: List<Resource>,
    val extraRequiredResources: List<Resource>,
    val notUsedResources: List<Resource>,
    val producedResources: List<Resource>
) {
    val resourcesAfterStage = notUsedResources + producedResources
}

data class ScenarioResourcesInfo(
    val providedResources: List<Resource>,
    val extraRequiredResources: Map<ScenarioStage, List<Resource>>,
    val notUsedResources: List<Resource>,
    val producedResources: List<Resource>
)