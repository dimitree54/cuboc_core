package cuboc.scenario

import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class Scenario(
    val stages: Set<ScenarioStage>,
    val dependencies: Map<ScenarioStage, Set<ScenarioStage>>,
) {
    val externalResourcesRequired: Map<ScenarioStage, List<Resource>>
    val producedForLaterStages: Map<ScenarioStage, Map<ScenarioStage, List<Resource>>>
    val extraProduced: Map<ScenarioStage, List<Resource>>

    init {
        checkValid()
        val perStageAvailableOutputs = stages.associateWith { it.output.toMutableList() }
        val externalDependencies = mutableMapOf<ScenarioStage, List<Resource>>()
        val producedForLater = mutableMapOf<ScenarioStage, MutableMap<ScenarioStage, MutableList<Resource>>>()
        calcDependencies(perStageAvailableOutputs, externalDependencies, producedForLater)
        extraProduced = perStageAvailableOutputs
        externalResourcesRequired = externalDependencies
        producedForLaterStages = producedForLater
    }

    private val outputStages: Set<ScenarioStage> = dependencies.keys - dependencies.values.flatten().toSet()
    private fun checkValid() {
        require(dependencies.values.all { it.isNotEmpty() }) {
            "Some stage depends on empty set, which is not allowed. Do not include such stages in dependencies"
        }
        require(stages.isNotEmpty()) { "Scenario must have at least one stage" }
        val allStagesInDependencies = dependencies.values.flatten().toSet() + dependencies.keys
        require(allStagesInDependencies.all { it in stages }) { "Unknown stage participating in dependencies" }
        outputStages.forEach { raiseIfCycle(it) }
    }

    private fun findResourcesInStagesOrBefore(
        stages: Set<ScenarioStage>,
        request: Resource,
        perStageAvailableOutputs: Map<ScenarioStage, MutableList<Resource>>
    ): Map<ScenarioStage, MutableList<Resource>> {
        val resourcesFound = mutableMapOf<ScenarioStage, MutableList<Resource>>()
        val searchQueue = stages.toMutableList()
        var amountToFulfill = request.amount
        while (searchQueue.isNotEmpty() && amountToFulfill > 0) {
            val resourcesFoundInStage = mutableListOf<Resource>()
            val stageToSearch = searchQueue.removeFirst()
            val stageRelevantOutputIndexes = perStageAvailableOutputs[stageToSearch]!!.mapIndexed { index, resource ->
                if (resource.ingredient == request.ingredient && resource.amount > 0) index else null
            }.filterNotNull()
            for (relevantOutputIndex in stageRelevantOutputIndexes) {
                val relevantOutput = perStageAvailableOutputs[stageToSearch]!![relevantOutputIndex]
                val availableAmount = relevantOutput.amount
                val amountToTake = minOf(availableAmount, amountToFulfill)
                val amountLeft = availableAmount - amountToTake
                amountToFulfill -= amountToTake
                perStageAvailableOutputs[stageToSearch]!![relevantOutputIndex] =
                    Resource(request.ingredient, amountLeft)
                resourcesFoundInStage.add(Resource(request.ingredient, amountToTake))
                if (amountToFulfill == 0.0) {
                    break
                }
            }
            resourcesFound[stageToSearch] = resourcesFoundInStage
            dependencies[stageToSearch]?.let { searchQueue.addAll(it) }
        }
        return resourcesFound
    }

    private fun calcExtraRequiredResources(
        request: Resource,
        existingResources: List<Resource>
    ): Resource? {
        require(existingResources.all { it.ingredient == request.ingredient })
        val fulfilledAmount = existingResources.sumOf { it.amount }
        val notFulfilledAmount = request.amount - fulfilledAmount
        if (notFulfilledAmount > 0) {
            return Resource(request.ingredient, notFulfilledAmount)
        }
        return null
    }

    private fun calcDependencies(
        perStageAvailableOutputs: Map<ScenarioStage, MutableList<Resource>>,
        allExternalDependencies: MutableMap<ScenarioStage, List<Resource>>,
        producedForLaterStages: MutableMap<ScenarioStage, MutableMap<ScenarioStage, MutableList<Resource>>>
    ) {
        val calcQueue = outputStages.toMutableList()
        while (calcQueue.isNotEmpty()) {
            val stage = calcQueue.removeFirst()
            val stageExternalDependencies = mutableListOf<Resource>()
            val stageStagesDependencies = mutableMapOf<ScenarioStage, MutableList<Resource>>()
            for (recipeInput in stage.recipe.inputs) {
                val dependencies =
                    findResourcesInStagesOrBefore(outputStages, recipeInput.resource, perStageAvailableOutputs)
                val externalDependency = calcExtraRequiredResources(recipeInput.resource, dependencies.values.flatten())
                dependencies.forEach { (dependencyStage, dependencyResources) ->
                    stageStagesDependencies.getOrPut(dependencyStage) { mutableListOf() }.addAll(dependencyResources)
                }
                externalDependency?.let { stageExternalDependencies.add(it) }
            }
            if (stageExternalDependencies.isNotEmpty()) {
                allExternalDependencies[stage] = stageExternalDependencies
            }
            if (stageStagesDependencies.isNotEmpty()) {
                for ((dependencyStage, dependencyResources) in stageStagesDependencies) {
                    producedForLaterStages.getOrPut(dependencyStage) { mutableMapOf() }
                        .getOrPut(stage) { mutableListOf() }.addAll(dependencyResources)
                }
            }
            dependencies[stage]?.let { calcQueue.addAll(it) }
        }
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
