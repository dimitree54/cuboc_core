package cuboc.database.firebase

import cuboc.ingredient.Ingredient
import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc_core.cuboc.database.search.SmartStringSearch
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where

class RecipesFirebase(private val db: FirebaseFirestore, private val idGenerator: IdGenerator) {
    private val collectionName = "recipes"
    private val ingredientsCollectionName = "recipeIngredients"
    private val recipeField = "recipe"
    private val recipeIdField = "recipeId"
    private val ingredientField = "ingredient"
    private val searchField = "searchableName"
    private val outputsSearchField = "searchableOutputs"

    suspend fun put(recipe: Recipe): UserRecipe {
        val id = idGenerator.generateId(recipe.name.toString())
        val userRecipe = UserRecipe(id, recipe)
        db.collection(collectionName).document(id).set(
            mapOf(
                recipeField to recipe,
                searchField to SmartStringSearch(recipe.name.toString()).normalisedQuery,
                outputsSearchField to recipe.outputs.map { SmartStringSearch(it.ingredient.name.toString()).normalisedQuery }
            )
        )
        for (ingredient in recipe.inputs.map { it.ingredient } + recipe.outputs.map { it.ingredient }) {
            db.collection(ingredientsCollectionName).add(
                mapOf(
                    ingredientField to ingredient,
                    recipeIdField to id,
                    searchField to SmartStringSearch(ingredient.name.toString()).normalisedQuery
                )
            )
        }
        return userRecipe
    }

    suspend fun get(id: String): UserRecipe {
        val document = db.collection(collectionName).document(id).get()
        return document.get(recipeField)
    }

    suspend fun remove(recipe: UserRecipe) {
        db.collection(collectionName).document(recipe.id).delete()
        val results = db.collection(ingredientsCollectionName).where(recipeIdField, equalTo = recipe.id).get()
        for (result in results.documents) {
            result.reference.delete()
        }
    }

    suspend fun searchByName(query: String): List<UserRecipe> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(collectionName)
            .where(searchField, equalTo = smartSearch.normalisedQuery).get()
        return results.documents.map { it.get(recipeField) }
    }

    suspend fun searchByOutput(query: String): List<UserRecipe> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(collectionName)
            .where(outputsSearchField, arrayContains = smartSearch.normalisedQuery).get()
        return results.documents.map { it.get(recipeField) }
    }

    suspend fun smartSearchByName(query: String): List<UserRecipe> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(collectionName)
            .where(searchField, greaterThan = smartSearch.searchBoundaries.first)
            .where(searchField, lessThan = smartSearch.searchBoundaries.second).get()
        return results.documents.map { it.get(recipeField) }
    }

    suspend fun smartSearchIngredients(query: String): List<Ingredient> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(ingredientsCollectionName)
            .where(searchField, greaterThan = smartSearch.searchBoundaries.first)
            .where(searchField, lessThan = smartSearch.searchBoundaries.second).get()
        return results.documents.map { it.get(ingredientField) }
    }
}
