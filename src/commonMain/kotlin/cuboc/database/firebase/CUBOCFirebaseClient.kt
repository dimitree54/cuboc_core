package cuboc.database.firebase

import cuboc.database.CUBOCDatabaseClient
import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress
import cuboc.scenario.ScenarioStageInProgress
import cuboc_core.cuboc.database.search.*
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.FirebaseFirestore

class CUBOCFirebaseClient(firestore: FirebaseFirestore, idGenerator: IdGenerator) : CUBOCDatabaseClient {
    private val resourcesDatabase = ResourcesFirebase(firestore, idGenerator)
    private val recipesDatabase = RecipesFirebase(firestore, idGenerator)

    private suspend fun smartSearchIngredientByName(query: String): List<Ingredient> {
        val ingredientsOfResources =
            resourcesDatabase.smartSearchByName(query).map { it.resource.ingredient }
        val ingredientsOfRecipes = recipesDatabase.smartSearchIngredients(query)
        return (ingredientsOfResources + ingredientsOfRecipes).toSet().toList()
    }

    override suspend fun search(request: SearchRequest): List<SearchResult> {
        return when (request.type) {
            SearchType.SmartAll -> {
                resourcesDatabase.smartSearchByName(request.query).map { ResourceSearchResult(it) } +
                        recipesDatabase.smartSearchByName(request.query).map { RecipeSearchResult(it) }
            }

            SearchType.SmartIngredients -> smartSearchIngredientByName(request.query).map {
                IngredientSearchResult(it)
            }

            SearchType.RecipesByOutput -> recipesDatabase.searchByOutput(request.query).map {
                RecipeSearchResult(it)
            }

            SearchType.Resources -> resourcesDatabase.searchByName(request.query).map {
                ResourceSearchResult(it)
            }

            else -> TODO()
        }
    }

    override suspend fun removeMyResource(resource: UserResource) {
        resourcesDatabase.remove(resource)
    }

    override suspend fun removeMyRecipe(recipe: UserRecipe) {
        recipesDatabase.remove(recipe)
    }

    override suspend fun addResource(resource: Resource): UserResource {
        return resourcesDatabase.put(resource)
    }

    override suspend fun addRecipe(recipe: Recipe): UserRecipe {
        return recipesDatabase.put(recipe)
    }

    override suspend fun chooseBestUserResources(
        resource: Resource
    ): List<PieceOfUserResource>? {
        val resourceRequests = mutableListOf<PieceOfUserResource>()
        var amountLeftToTake = resource.amount
        val searchRequest = SearchRequest(resource.ingredient.name.toString(), SearchType.Resources)
        for (searchResult in search(searchRequest)) {
            val pieceOfUserResource = (searchResult as ResourceSearchResult).pieceOfUserResource
            if (pieceOfUserResource.amount >= amountLeftToTake) {
                resourceRequests.add(PieceOfUserResource(pieceOfUserResource.userResource, amountLeftToTake))
                amountLeftToTake = 0.0
                break
            } else {
                resourceRequests.add(pieceOfUserResource) // take all of it
                amountLeftToTake -= pieceOfUserResource.amount
            }
        }
        return if (amountLeftToTake > 0) null else resourceRequests
    }

    private fun getCost(recipe: Recipe): Double {
        return recipe.instruction.durationMinutes.toDouble()
    }

    private fun getCost(pieceOfUserResource: PieceOfUserResource): Double? {
        return 0.0
    }

    private suspend fun getCost(resource: Resource): Double? {
        return chooseBestUserResources(resource)?.sumOf { getCost(it) ?: return null } ?: return null
    }

    override suspend fun getCost(scenario: Scenario): Double? {
        val recipeCost = scenario.stages.sumOf { getCost(it.recipe) }
        val resourcesCost = scenario.externalResourcesRequired.values.flatten().sumOf { getCost(it) ?: return null }
        val returnedCost = scenario.extraProduced.values.flatten().sumOf { getCost(it) ?: return null }
        return recipeCost + resourcesCost - returnedCost
    }

    override suspend fun getMyRecipes(): List<UserRecipe> {
        TODO("Not yet implemented")
    }

    override suspend fun getMyScenariosInProgress(): List<ScenarioInProgress> {
        TODO("Not yet implemented")
    }

    override suspend fun getMyStagesInProgress(): List<ScenarioStageInProgress> {
        TODO("Not yet implemented")
    }

    override suspend fun getMyResources(): List<PieceOfUserResource> {
        TODO("Not yet implemented")
    }
}

