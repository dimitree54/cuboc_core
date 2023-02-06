package cuboc_core.cuboc.database.firebase

import cuboc.database.CUBOCDatabase
import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.UserRecipe
import cuboc_core.cuboc.database.UserResource
import cuboc_core.cuboc.database.search.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

class CUBOCFirebase : CUBOCDatabase {
    private val db = Firebase.firestore
    private val resourcesDatabase = ResourcesFirebase(db)
    private val recipesDatabase = RecipesFirebase(db)

    // only for admin
    override suspend fun execute(scenario: Scenario): PieceOfResource? {
        val userId = "test_user"
        var success = false
        db.runTransaction {
            for (recipeInput in scenario.recipe.inputs) {
                val resourceRequests = scenario.resources[recipeInput]!!
                for (resourceRequest in resourceRequests) {
                    require(resourcesDatabase.reserve(resourceRequest, userId))
                }
            }
            success = true
        }
        if (!success) {
            return null
        }
        success = false
        var requestedResource: PieceOfResource? = null
        db.runTransaction {
            for (recipeOutput in scenario.recipe.outputs) {
                val resourcePrototype = Resource(recipeOutput.ingredient, recipeOutput.amount)
                val resource = resourcesDatabase.put(resourcePrototype)
                if (recipeOutput.ingredient == scenario.request.ingredient) {
                    requestedResource = PieceOfResource(resource, scenario.request.amount)
                    resourcesDatabase.reserve(requestedResource!!, userId)
                }
            }
            success = true
        }
        if (!success) {
            for (recipeInput in scenario.recipe.inputs) {
                val resourceRequests = scenario.resources[recipeInput]!!
                for (resourceRequest in resourceRequests) {
                    resourcesDatabase.release(resourceRequest, userId)
                }
            }
            return null
        }
        require(resourcesDatabase.getReservedAmount(requestedResource!!, userId))
        return requestedResource!!
    }

    private suspend fun searchIngredientByName(query: String): List<Ingredient> {
        val ingredientsOfResources =
            resourcesDatabase.searchByName(query).map { it.ingredient }.toSet()
        val ingredientsOfInputs =
            recipesDatabase.searchByInput(query).flatMap { it.inputs }.map { it.ingredient }.toSet()
        val ingredientsOfOutputs =
            recipesDatabase.searchByOutput(query).flatMap { it.outputs }.map { it.ingredient }
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

            else -> TODO()
        }
    }

    override suspend fun removeResource(resource: UserResource): Boolean {
        return resourcesDatabase.remove(resource)
    }

    override suspend fun removeRecipe(recipe: UserRecipe): Boolean {
        return recipesDatabase.remove(recipe)
    }

    override suspend fun addResource(resource: Resource): UserResource {
        return resourcesDatabase.put(resource)
    }

    override suspend fun addRecipe(recipe: Recipe): UserRecipe {
        return recipesDatabase.put(recipe)
    }
}
