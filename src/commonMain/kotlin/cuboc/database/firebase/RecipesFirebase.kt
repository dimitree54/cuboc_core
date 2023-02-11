package cuboc_core.cuboc.database.firebase

import cuboc.recipe.Recipe
import cuboc.recipe.UserRecipe
import cuboc_core.utility.IdGenerator
import cuboc_core.utility.Report
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where

class RecipesFirebase(private val db: FirebaseFirestore, private val idGenerator: IdGenerator) {
    private val collectionName = "recipes"

    suspend fun put(recipe: Recipe): UserRecipe {
        val id = idGenerator.generateId(recipe.name.toString())
        val userRecipe = UserRecipe(id, recipe)
        db.collection(collectionName).document(id).set(
            "recipe" to recipe
        )
        return userRecipe
    }

    private fun decode(document: DocumentSnapshot): UserRecipe {
        return document.get("recipe")
    }

    suspend fun get(id: String): UserRecipe {
        val document = db.collection(collectionName).document(id).get()
        return decode(document)
    }

    suspend fun remove(recipe: UserRecipe): Boolean {
        db.collection(collectionName).document(recipe.id).delete()
        return true
    }

    suspend fun searchByName(query: String): List<UserRecipe> {
        val searchQuery = query.lowercase().trim()
        val lastChar = searchQuery.last()
        val smartSearchGreaterThan = searchQuery.dropLast(1) + (lastChar - 1)
        val smartSearchLessThan = searchQuery.dropLast(1) + (lastChar + 1)
        val results = db.collection(collectionName).where("searchableName", greaterThan = smartSearchGreaterThan)
            .where("searchableName", lessThan = smartSearchLessThan).get()
        return results.documents.map(::decode)
    }

    suspend fun searchByInput(query: String): List<UserRecipe> {
        val results =
            db.collection(collectionName).where("allInputNames", arrayContains = query).get()
        return results.documents.map { it.get("recipe") }
    }

    suspend fun searchByOutput(query: String): List<UserRecipe> {
        val results =
            db.collection(collectionName).where("allOutputNames", arrayContains = query).get()
        return results.documents.map { it.get("recipe") }
    }

    suspend fun searchByAuthor(authorId: String): List<UserRecipe> {
        TODO()
    }

    private fun decodeReports(document: DocumentSnapshot): List<Report> {
        if (document.contains("reports")) {
            return document.get("reports")
        }
        return emptyList()
    }

    suspend fun report(recipeToReport: UserRecipe, report: Report): Boolean {
        val id = recipeToReport.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val reports = decodeReports(document)
        documentReference.update("reports" to reports + report)
        return true
    }
}
