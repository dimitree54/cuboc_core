package cuboc.scenario

import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class Scenario(
    val stages: Set<ScenarioStage>,
    val dependencies: Map<ScenarioStage, Set<ScenarioStage>>,
) {
    private val startingStages: Set<ScenarioStage> = stages - dependencies.keys
    private val outputStages: Set<ScenarioStage> = dependencies.keys - dependencies.values.flatten().toSet()
    val externalResourcesRequired: Map<ScenarioStage, List<Resource>> = getExternalResourcesRequired()
    private val invertedRequirements = getInvertedRequirements()

    init {
        checkValid()
    }

    private fun getExternalResourcesRequired(): Map<ScenarioStage, List<Resource>> {
        return startingStages.flatMap { getExtraRequiredResources(it).toList() }.toMap()
    }

    private fun getInvertedRequirements(): Map<ScenarioStage, Set<ScenarioStage>> {
        val invertedRequirements = mutableMapOf<ScenarioStage, MutableSet<ScenarioStage>>()
        for ((stage, dependencies) in dependencies) {
            for (dependency in dependencies) {
                invertedRequirements.getOrPut(dependency) { mutableSetOf() }.add(stage)
            }
        }
        return invertedRequirements
    }

    private fun checkValid() {
        require(dependencies.values.all { it.isNotEmpty() }) {
            "Some stage depends on empty set, which is not allowed. Do not include such stages in dependencies"
        }
        require(stages.isNotEmpty()) { "Scenario must have at least one stage" }
        val allStagesInDependencies = dependencies.values.flatten().toSet() + dependencies.keys
        require(allStagesInDependencies.all { it in stages }) { "Unknown stage participating in dependencies" }
        outputStages.forEach { raiseIfCycle(it) }
    }

    private fun getExtraRequiredResources(
        stage: ScenarioStage,
        providedResources: List<Resource> = emptyList()
    ): Map<ScenarioStage, List<Resource>> {
        return getExtraRequiredPerStagesAndNotUsedResources(stage, providedResources).first
    }

    private fun getExtraRequiredPerStagesAndNotUsedResources(
        stage: ScenarioStage,
        providedResources: List<Resource>
    ): Pair<Map<ScenarioStage, List<Resource>>, List<Resource>> {
        require(stage in stages) { "Unknown stage" }
        val notUsedAfterStage = providedResources.toMutableList()
        val extraRequiredForStage = mutableListOf<Resource>()
        for (recipeInput in stage.recipe.inputs) {
            var amountLeftToFulfill = recipeInput.amount
            val relevantIndexes = notUsedAfterStage.mapIndexed { index, resource ->
                if (resource.ingredient == recipeInput.ingredient) index else null
            }.filterNotNull()
            for (relevantIndex in relevantIndexes) {
                val relevantResource = notUsedAfterStage[relevantIndex]
                val amountAvailable = relevantResource.amount
                val amountToTake = minOf(amountLeftToFulfill, amountAvailable)
                val amountLeft = amountAvailable - amountToTake
                amountLeftToFulfill -= amountToTake
                notUsedAfterStage[relevantIndex] = Resource(relevantResource.ingredient, amountLeft)
            }
            if (amountLeftToFulfill > 0) {
                extraRequiredForStage.add(Resource(recipeInput.ingredient, amountLeftToFulfill))
            }
        }
        val outputResourcesAfterStage = stage.recipe.outputs.map { Resource(it.ingredient, it.amount) }

        val extraRequiredPerStages = mutableMapOf<ScenarioStage, List<Resource>>()
        extraRequiredPerStages[stage] = extraRequiredForStage
        var notUsedAtAll = notUsedAfterStage.toList()
        var outputsLeft = outputResourcesAfterStage.toList()
        invertedRequirements[stage]?.let { dependingStages ->
            for (dependingStage in dependingStages) {
                val (extraRequiredPerLaterStages, notUsedJoined) =
                    getExtraRequiredPerStagesAndNotUsedResources(dependingStage, notUsedAtAll + outputsLeft)
                notUsedAtAll = notUsedJoined.subList(0, notUsedAtAll.size)
                outputsLeft = notUsedJoined.subList(notUsedAtAll.size, notUsedAtAll.size + outputsLeft.size)
                extraRequiredPerStages += extraRequiredPerLaterStages
            }
        }
        return extraRequiredPerStages to notUsedAtAll
    }

    private fun raiseIfCycle(stage: ScenarioStage, stagesAbove: Set<ScenarioStage> = emptySet()) {
        dependencies[stage]?.let {
            for (child in it) {
                if (child in stagesAbove) {
                    throw IllegalArgumentException("Cycle detected")
                }
                raiseIfCycle(child, stagesAbove + stage)
            }
        }
    }

    fun addSequentialStage(stage: ScenarioStage): Scenario {
        require(stage !in stages) { "Duplicating stages are not supported" }
        return Scenario(stages + stage, dependencies + (stage to outputStages))
    }

    fun joinParallel(scenario: Scenario): Scenario {
        require(stages.intersect(scenario.stages).isEmpty()) { "Duplicating stages are not supported" }
        return Scenario(stages + scenario.stages, dependencies + scenario.dependencies)
    }
}
