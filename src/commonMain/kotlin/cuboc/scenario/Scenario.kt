package cuboc.scenario

import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
data class Scenario(
    val stages: Set<ScenarioStage>,
    val dependencyIds: Map<String, Set<String>>,
) {
    val externalResourcesRequired: Map<RecipeInput, Resource>
    val producedResources: Set<Resource>
    val lastStageIds: Set<String> = dependencyIds.keys - dependencyIds.values.flatten().toSet()

    init {
        require(stages.isNotEmpty()) { "Scenario must have at least one stage" }
        val allStageIds = stages.map { it.id }.toSet()
        require(allStageIds.size == stages.size) { "Duplicate stage id in scenario" }
        val allIdsInDependencies = dependencyIds.values.flatten().toSet() + dependencyIds.keys
        require(allIdsInDependencies == allStageIds) { "Invalid ids of dependencies" }
        require(checkDependencyGraphValid()) { "Dependency graph invalid" }

        val externalResourcesRequired = mutableMapOf<RecipeInput, Resource>()
        val producedResources = mutableSetOf<Resource>()
        for (lastStageId in lastStageIds) {
            val (stageConsumedResources, stageProducedResources) = getConsumedProducedResources(lastStageId)
            externalResourcesRequired += stageConsumedResources
            for (producedResource in stageProducedResources) {
                addNewProducedResource(producedResources, producedResource)
            }
        }
        this.externalResourcesRequired = externalResourcesRequired
        this.producedResources = producedResources
    }

    private fun addNewProducedResource(existingProducedResources: MutableSet<Resource>, newProducedResource: Resource) {
        existingProducedResources.add(
            existingProducedResources.firstOrNull { it.ingredient == newProducedResource.ingredient }?.let {
                existingProducedResources.remove(it)
                Resource(
                    newProducedResource.ingredient,
                    newProducedResource.amount + it.amount
                )
            } ?: newProducedResource
        )
    }

    private fun getConsumedProducedResources(stageId: String): Pair<Map<RecipeInput, Resource>, Set<Resource>> {
        val consumedResources = mutableMapOf<RecipeInput, Resource>()
        val producedResources = mutableSetOf<Resource>()
        for (childId in dependencyIds[stageId] ?: emptySet()) {
            val (childConsumedResources, childProducedResources) = getConsumedProducedResources(childId)
            consumedResources += childConsumedResources
            for (childProducedResource in childProducedResources) {
                addNewProducedResource(producedResources, childProducedResource)
            }
        }
        val recipe = stages.first { it.id == stageId }.recipe
        for (recipeInput in recipe.inputs) {
            val existingResource = producedResources.firstOrNull { it.ingredient == recipeInput.ingredient }
            val extraAmountRequired = existingResource?.let {
                val existingAmount = it.amount
                val consumedAmount = min(existingAmount, recipeInput.amount)
                val amountLeft = existingAmount - consumedAmount
                producedResources.remove(it)
                if (amountLeft > 0) {
                    producedResources.add(Resource(recipeInput.ingredient, amountLeft))
                }
                recipeInput.amount - consumedAmount
            } ?: recipeInput.amount
            if (extraAmountRequired > 0) {
                consumedResources[recipeInput] = Resource(recipeInput.ingredient, extraAmountRequired)
            }
        }
        for (recipeOutput in recipe.outputs) {
            addNewProducedResource(producedResources, recipeOutput.resource)
        }
        return consumedResources to producedResources
    }


    private fun checkDependencyGraphValid(): Boolean {
        val visited = mutableSetOf<String>()
        val toVisit = lastStageIds.toMutableSet()
        while (toVisit.isNotEmpty()) {
            val id = toVisit.first()
            toVisit.remove(id)
            if (id in visited) {
                return false
            }
            visited.add(id)
            toVisit.addAll(dependencyIds[id] ?: emptySet())
        }
        return visited.size == stages.size
    }

    fun addSequentialStage(stage: ScenarioStage): Scenario {
        return Scenario(stages + stage, dependencyIds + (stage.id to lastStageIds))
    }

    fun joinParallel(scenario: Scenario): Scenario {
        return Scenario(stages + scenario.stages, dependencyIds + scenario.dependencyIds)
    }
}
