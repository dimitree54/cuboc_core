package cuboc_core.cuboc.database.firebase

import cuboc.database.CUBOCDatabase
import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource
import cuboc.recipe.ComplexRecipe
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.search.*
import cuboc_core.cuboc.ingredient.UserResource
import cuboc_core.cuboc.recipe.UserRecipe
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

class CUBOCFirebase : CUBOCDatabase {
    private val db = Firebase.firestore
    private val resourcesDatabase = ResourcesFirebase(db)
    private val recipesDatabase = RecipesFirebase(db)

    // only for admin
    private suspend fun execute(
        recipe: Recipe,
        reservedResources: Map<RecipeInput, List<PieceOfResource>>,
        requesterId: String
    ): List<UserResource> {
        val producedResources = mutableListOf<UserResource>()
        if (recipe is ComplexRecipe) {
            for (subRecipe in recipe.stages) {
                producedResources.addAll(execute(subRecipe, reservedResources, requesterId))
            }
        }
        var success = false
        db.runTransaction {
            for (recipeInput in recipe.inputs) {
                val reservedResourcesForInput = reservedResources[recipeInput]!!
                for (resourceRequest in reservedResourcesForInput) {
                    require(resourcesDatabase.getReservedAmount(resourceRequest, requesterId))
                }
            }
            for (recipeOutput in recipe.outputs) {
                val resource = resourcesDatabase.put(Resource(recipeOutput.ingredient, recipeOutput.amount))
                resourcesDatabase.reserve(PieceOfResource(resource, recipeOutput.amount), requesterId)
                producedResources.add(resource)
            }
            success = true
        }
        if (success) {
            println("Execution of recipe ${recipe.name} successful")
        } else {
            println("Execution of recipe ${recipe.name}  failed")
        }
        return producedResources
    }

    // only for admin
    override suspend fun execute(scenario: Scenario, requesterId: String): PieceOfResource? {
        println("Start scenario execution")
        var success = false
        db.runTransaction {
            for (recipeInput in scenario.recipe.inputs) {
                val resourceRequests = scenario.resources[recipeInput]!!
                for (resourceRequest in resourceRequests) {
                    require(resourcesDatabase.reserve(resourceRequest, requesterId))
                }
            }
            success = true
        }
        if (success) {
            println("Reservation successful")
        } else {
            println("Reservation failed")
            return null
        }
        var requestedPieceOfResource: PieceOfResource? = null
        val producedResources = execute(scenario.recipe, scenario.resources, requesterId)
        for (producedResource in producedResources) {
            if (producedResource.resource.ingredient == scenario.request.ingredient) {
                requestedPieceOfResource = PieceOfResource(producedResource, scenario.request.amount)
                resourcesDatabase.getReservedAmount(requestedPieceOfResource, requesterId)
            }
            val pieceOfResource = PieceOfResource(producedResource, producedResource.resource.amount)
            resourcesDatabase.release(pieceOfResource, requesterId)
        }
        return requestedPieceOfResource!!
    }

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
