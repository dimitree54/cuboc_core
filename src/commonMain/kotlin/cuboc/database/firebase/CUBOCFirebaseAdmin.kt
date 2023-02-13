package cuboc.database.firebase

import cuboc.database.CUBOCDatabaseAdmin
import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.UserRecipe
import cuboc.scenario.*
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.FirebaseFirestore

class CUBOCFirebaseAdmin(
    private val firestore: FirebaseFirestore,
    private val idGenerator: IdGenerator
) :
    CUBOCDatabaseAdmin {
    private val clientDatabase: CUBOCFirebaseClient = CUBOCFirebaseClient(firestore, idGenerator)
    private val resourcesDatabase = ResourcesFirebaseAdmin(firestore, idGenerator)
    private val scenariosDatabase = ScenariosFirebase(firestore)

    private suspend fun registerStageOutputResources(stage: ScenarioStageInProgress): List<UserResource> {
        val producedResources = mutableListOf<UserResource>()
        try {
            firestore.runTransaction {
                for (recipeOutput in stage.recipe.outputs) {
                    val producedResource = resourcesDatabase.put(Resource(recipeOutput.ingredient, recipeOutput.amount))
                    require(
                        resourcesDatabase.reserve(
                            PieceOfUserResource(producedResource, recipeOutput.amount),
                            getRequesterId()
                        )
                    ) {
                        "Failed to reserve produced resource ${recipeOutput.ingredient.name}"
                    }
                    producedResources.add(producedResource)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return producedResources
    }

    private suspend fun consumeStageInputResources(stage: ScenarioStageInProgress): Boolean {
        try {
            firestore.runTransaction {
                for (recipeInput in stage.recipe.inputs) {
                    val reservedResources = stage.inputResources[recipeInput]!!
                    for (resourceResource in reservedResources) {
                        require(
                            resourcesDatabase.consumeReserved(
                                resourceResource,
                                getRequesterId()
                            )
                        ) { "Failed to consume reserved resource ${recipeInput.ingredient.name}" }
                    }
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private suspend fun reserveScenarioInputResources(scenario: Scenario): Map<RecipeInput, List<PieceOfUserResource>>? {
        val requesterId = getRequesterId()
        val externalResources = mutableMapOf<RecipeInput, List<PieceOfUserResource>>()
        try {
            firestore.runTransaction {
                for ((recipeInput, externalResourceRequest) in scenario.externalResourcesRequired) {
                    val userResources = clientDatabase.chooseBestUserResources(externalResourceRequest)
                        ?: throw Exception("External resource for ${recipeInput.ingredient.name} not found")
                    userResources.forEach {
                        require(
                            resourcesDatabase.reserve(
                                it,
                                requesterId
                            )
                        ) { "External resource for ${recipeInput.ingredient.name} can not be reserved" }
                    }
                    externalResources[recipeInput] = userResources
                }
            }
        } catch (e: Exception) {
            return null
        }
        return externalResources
    }

    private fun getRequesterId(): String {
        return "admin"
    }

    override suspend fun launchScenario(request: Resource, scenario: Scenario): ScenarioInProgress? {
        val externalResources = reserveScenarioInputResources(scenario) ?: return null
        val scenarioInProgress = ScenarioInProgress(
            idGenerator.generateId(request.ingredient.name.toString()),
            request,
            scenario,
            externalResources
        )
        scenariosDatabase.put(scenarioInProgress)
        launchNextStages(scenarioInProgress.id)
        return scenarioInProgress
    }

    private suspend fun notifyCrafter(stage: ScenarioStageInProgress): Boolean {
        TODO()
    }

    private suspend fun notifyScenarioFinished(scenario: ScenarioInProgress): Boolean {
        TODO()
    }

    private suspend fun launchNextStages(scenarioId: String): Boolean {
        TODO()
    }

    private suspend fun launchStage(
        scenarioInProgress: ScenarioInProgress,
        stage: ScenarioStage
    ): ScenarioStageInProgress? {
        val stageId = stage.id
        val resourcesReservedForStage = mutableListOf<PieceOfUserResource>()
        for (dependencyId in scenarioInProgress.scenario.dependencyIds[stageId] ?: emptyList()) {
            val dependencyStage = scenariosDatabase.getStage(scenarioInProgress.id, dependencyId) ?: return null
            if (dependencyStage.state != ScenarioStageStatus.DONE) return null
            val resourcesLeftFromPreviousStage =
                scenariosDatabase.getResourcesAfterStage(scenarioInProgress.id, dependencyId) ?: return null
            resourcesReservedForStage.addAll(resourcesLeftFromPreviousStage)
        }
        for (recipeInput in stage.recipe.inputs) {
            val externalResources = scenarioInProgress.externalResources[recipeInput] ?: continue
            resourcesReservedForStage.addAll(externalResources)
        }
        val stageInProgress = ScenarioStageInProgress(
            stageId,
            stage,
            resourcesReservedForStage,
            ScenarioStageStatus.IN_PROGRESS
        )
        consumeStageInputResources(stageInProgress).let { if (!it) return null }
        notifyCrafter(stageInProgress).let { if (!it) return null }
        return stageInProgress
    }

    suspend fun reportStageFinished(scenarioId: String, stageId: String): Boolean {
        val scenario = scenariosDatabase.getScenario(scenarioId) ?: return false
        val stage = scenariosDatabase.getStage(scenarioId, stageId) ?: return false
        val producedResources = registerStageOutputResources(stage).map { PieceOfUserResource(it, it.amount) }
        val resourcesLeftFromPreviousStages = mutableListOf<PieceOfUserResource>()
        for (previousStageId in scenario.scenario.dependencyIds[stageId] ?: emptyList()) {
            val previousStage = scenariosDatabase.getStage(scenarioId, previousStageId) ?: return false
            resourcesLeftFromPreviousStages.addAll(previousStage.extraResourcesReserved)
        }
        scenariosDatabase.finishStage(scenarioId, stageId, producedResources + resourcesLeftFromPreviousStages)
            .let { if (!it) return false }
        launchNextStages(scenarioId).let { if (!it) return false }
        return true
    }

    private suspend fun consumeRequestedResource(
        scenario: ScenarioInProgress,
        resourcesAfterScenario: List<PieceOfUserResource>
    ): List<PieceOfUserResource> {
        val ingredientRequested = scenario.request.ingredient
        var amountRequested = scenario.request.amount
        val resourcesLeft = mutableListOf<PieceOfUserResource>()
        for (resourceAfterScenario in resourcesAfterScenario) {
            if (resourceAfterScenario.ingredient == ingredientRequested && amountRequested > 0) {
                val amountAfterScenario = resourceAfterScenario.amount
                val amountToConsume = minOf(amountRequested, amountAfterScenario)
                val amountLeft = amountAfterScenario - amountToConsume
                amountRequested -= amountToConsume
                val resourceToConsume = PieceOfUserResource(resourceAfterScenario.userResource, amountToConsume)
                resourcesDatabase.consumeReserved(resourceToConsume, getRequesterId())
                if (amountLeft > 0) {
                    resourcesLeft.add(
                        PieceOfUserResource(
                            resourceAfterScenario.userResource,
                            amountLeft
                        )
                    )
                }
            } else {
                resourcesLeft.add(resourceAfterScenario)
            }
        }
        return resourcesLeft
    }

    private suspend fun releaseResourcesLeft(resourcesLeft: List<PieceOfUserResource>): Boolean {
        for (resource in resourcesLeft) {
            resourcesDatabase.release(resource, getRequesterId())
        }
        return true
    }

    private suspend fun finishScenario(scenarioId: String): Boolean {
        val scenario = scenariosDatabase.getScenario(scenarioId) ?: return false
        val resourcesAfterScenario = mutableListOf<PieceOfUserResource>()
        for (lastStageId in scenario.scenario.lastStageIds) {
            val lastStage = scenariosDatabase.getStage(scenarioId, lastStageId) ?: return false
            resourcesAfterScenario.addAll(lastStage.extraResourcesReserved)
        }
        val resourcesLeft = consumeRequestedResource(scenario, resourcesAfterScenario)
        releaseResourcesLeft(resourcesLeft).let { if (!it) return false }
        scenariosDatabase.remove(scenarioId)
        notifyScenarioFinished(scenario).let { if (!it) return false }
        return true
    }

    override suspend fun removeRecipe(recipe: UserRecipe): Boolean {
        return clientDatabase.removeMyRecipe(recipe)
    }

    override suspend fun removeResource(resource: UserResource): Boolean {
        return clientDatabase.removeMyResource(resource)
    }
}