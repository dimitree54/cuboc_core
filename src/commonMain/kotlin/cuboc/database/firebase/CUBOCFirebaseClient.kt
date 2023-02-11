package cuboc_core.cuboc.database.firebase

import cuboc.database.CUBOCDatabaseClient
import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc.scenario.Scenario
import cuboc.scenario.ScenarioInProgress
import cuboc_core.cuboc.database.search.*
import cuboc_core.utility.IdGenerator
import cuboc_core.utility.Report
import dev.gitlive.firebase.firestore.FirebaseFirestore

class CUBOCFirebaseClient(firestore: FirebaseFirestore, idGenerator: IdGenerator) : CUBOCDatabaseClient {
    private val resourcesDatabase = ResourcesFirebase(firestore, idGenerator)
    private val recipesDatabase = RecipesFirebase(firestore, idGenerator)
    private val scenariosDatabase = ScenariosFirebase(firestore)

    private suspend fun searchIngredientByName(query: String): List<Ingredient> {
        val ingredientsOfResources =
            resourcesDatabase.searchByName(query).map { it.resource.ingredient }.toSet()
        val ingredientsOfInputs =
            recipesDatabase.searchByInput(query).flatMap { it.inputs }.map { it.ingredient }
                .filter { it.name.toString() == query }
                .toSet()
        val ingredientsOfOutputs =
            recipesDatabase.searchByOutput(query).flatMap { it.outputs }.map { it.ingredient }
                .filter { it.name.toString() == query }
                .toSet()
        return (ingredientsOfResources + ingredientsOfInputs + ingredientsOfOutputs).toList()
    }

    override suspend fun search(request: SearchRequest): List<SearchResult> {
        return when (request.type) {
            SearchType.All -> {
                val resources =
                    resourcesDatabase.searchByName(request.query).map { ResourceSearchResult(it) }
                val recipes =
                    recipesDatabase.searchByName(request.query).map { RecipeSearchResult(it) }
                resources + recipes
            }

            SearchType.Ingredients -> searchIngredientByName(request.query).map {
                IngredientSearchResult(
                    it
                )
            }

            SearchType.RecipesByOutput -> recipesDatabase.searchByOutput(request.query).map {
                RecipeSearchResult(
                    it
                )
            }

            SearchType.Resources -> resourcesDatabase.searchByName(request.query).map {
                ResourceSearchResult(
                    it
                )
            }

            else -> TODO()
        }
    }

    override suspend fun removeMyResource(resource: UserResource): Boolean {
        return resourcesDatabase.remove(resource)
    }

    override suspend fun removeMyRecipe(recipe: UserRecipe): Boolean {
        return recipesDatabase.remove(recipe)
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
        var amountLeft = resource.amount
        val searchRequest = SearchRequest(resource.ingredient.name.toString(), SearchType.Resources)
        for (searchResult in search(searchRequest)) {
            val pieceOfUserResource = (searchResult as ResourceSearchResult).pieceOfUserResource
            if (pieceOfUserResource.amount >= amountLeft) {
                resourceRequests.add(PieceOfUserResource(pieceOfUserResource.userResource, amountLeft))
                amountLeft = 0.0
                break
            } else {
                resourceRequests.add(pieceOfUserResource) // take all of it
                amountLeft -= pieceOfUserResource.amount
            }
        }
        return if (amountLeft > 0) null else resourceRequests
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
        val resourcesCost = scenario.externalResourcesRequired.values.sumOf { getCost(it) ?: return null }
        return recipeCost + resourcesCost
    }

    override suspend fun reportRecipe(recipe: UserRecipe, report: Report): Boolean {
        return recipesDatabase.report(recipe, report)
    }

    override suspend fun reportResource(resource: UserResource, report: Report): Boolean {
        return resourcesDatabase.report(resource, report)
    }

    private fun getMyUserId(): String {
        return "test"
    }

    override suspend fun getMyRecipes(): List<UserRecipe> {
        return recipesDatabase.searchByAuthor(getMyUserId())
    }

    override suspend fun getMyResources(): List<PieceOfUserResource> {
        return resourcesDatabase.searchByAuthor(getMyUserId())
    }

    override suspend fun getMyScenarios(): List<ScenarioInProgress> {
        return scenariosDatabase.searchByRequester(getMyUserId())
    }

    override suspend fun launchScenario(scenario: Scenario): ScenarioInProgress? {
        return null
    }
}

