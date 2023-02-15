package cuboc.database.firebase

import cuboc.database.CUBOCDatabaseAdmin
import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.UserRecipe
import cuboc.scenario.*
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class CUBOCFirebaseAdmin(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val idGenerator: IdGenerator = IdGenerator()
) :
    CUBOCDatabaseAdmin {
    private val clientDatabase: CUBOCFirebaseClient = CUBOCFirebaseClient(firestore, idGenerator)
    private val resourcesDatabase = ResourcesFirebaseAdmin(firestore, idGenerator)
    private val scenariosDatabase = ScenariosFirebase(firestore)

    private fun getRequesterId(): String {
        return "test_requester_id"
    }

    override suspend fun launchScenario(request: List<Resource>, scenario: Scenario): ScenarioInProgress {
        val reservedResources = scenario.externalResourcesRequired.map { (stage, requiredResources) ->
            stage to requiredResources.flatMap {
                clientDatabase.chooseBestUserResources(it)
                    ?: throw Exception("Scenario impossible, can not find resources for it")
            }
        }.toMap()
        val reserverId = getRequesterId()
        firestore.runTransaction {
            reservedResources.values.flatten().forEach { resourcesDatabase.reserve(it, reserverId) }
        }
        val scenarioInProgress = ScenarioInProgress(
            idGenerator.generateId(request.first().ingredient.name.toString()),
            request,
            scenario,
            reservedResources
        )
        scenariosDatabase.put(scenarioInProgress)
        return scenarioInProgress
    }

    private suspend fun launchNextStages(scenarioInProgress: ScenarioInProgress) {
        val stagesInProgress = scenariosDatabase.getStages(scenarioInProgress.id)
        val nextStages = scenarioInProgress.getStagesReadyToStart(stagesInProgress.values)
        if (nextStages.isEmpty()) {
            finishScenario(scenarioInProgress)
        } else {
            nextStages.forEach {
                launchStage(scenarioInProgress, it)
            }
        }
    }

    private suspend fun finishScenario(scenarioInProgress: ScenarioInProgress) {
        scenariosDatabase.remove(scenarioInProgress.id)
    }

    private suspend fun launchStage(scenarioInProgress: ScenarioInProgress, scenarioStage: ScenarioStage) {
        val externalResourcesReserved = scenarioInProgress.externalResources[scenarioStage]!!
        val resourcesFromPreviousStages =
            scenariosDatabase.getResourcesForStage(scenarioInProgress.id, scenarioStage.id)
        val stageInProgress = ScenarioStageInProgress(
            scenarioInProgress.id,
            idGenerator.generateId(),
            scenarioStage,
            externalResourcesReserved + resourcesFromPreviousStages,
            ScenarioStageStatus.IN_PROGRESS
        )
        scenariosDatabase.startStage(scenarioInProgress.id, stageInProgress)
    }

    override suspend fun finishStage(scenarioInProgress: ScenarioInProgress, stageInProgress: ScenarioStageInProgress) {
        releaseExtraResources(scenarioInProgress, stageInProgress)
        val producedForLater = registerResourcesProducedForLater(scenarioInProgress, stageInProgress)
        val finishedStage = stageInProgress.getWithUpdatedState(ScenarioStageStatus.DONE)
        scenariosDatabase.finishStage(stageInProgress.scenarioId, finishedStage, producedForLater)
        launchNextStages(scenarioInProgress)
    }

    private suspend fun releaseExtraResources(
        scenarioInProgress: ScenarioInProgress,
        stageInProgress: ScenarioStageInProgress
    ) {
        val extraProduced = scenarioInProgress.scenario.extraProduced[stageInProgress.stage] ?: emptyList()
        for (resource in extraProduced) {
            resourcesDatabase.put(resource)
        }
    }

    private suspend fun registerResourcesProducedForLater(
        scenarioInProgress: ScenarioInProgress,
        stageInProgress: ScenarioStageInProgress
    ): MutableMap<String, List<PieceOfUserResource>> {
        val reserverId = getRequesterId()
        val producedForLaterResources =
            scenarioInProgress.scenario.producedForLaterStages[stageInProgress.stage] ?: emptyMap()
        val producedForLater = mutableMapOf<String, List<PieceOfUserResource>>()
        for ((forStage, resources) in producedForLaterResources) {
            val producedForLaterStage = mutableListOf<PieceOfUserResource>()
            for (resource in resources) {
                firestore.runTransaction {
                    val userResource = resourcesDatabase.put(resource)
                    val pieceOfUserResource = PieceOfUserResource(userResource, userResource.amount)
                    resourcesDatabase.reserve(pieceOfUserResource, reserverId)
                    producedForLaterStage.add(pieceOfUserResource)
                }
            }
            producedForLater[forStage.id] = producedForLaterStage
        }
        return producedForLater
    }

    override suspend fun cancelScenario(scenarioInProgress: ScenarioInProgress) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelStage(scenarioStageInProgress: ScenarioStageInProgress) {
        TODO("Not yet implemented")
    }

    override suspend fun removeResource(resource: UserResource) {
        TODO("Not yet implemented")
    }

    override suspend fun removeRecipe(recipe: UserRecipe) {
        TODO("Not yet implemented")
    }
}