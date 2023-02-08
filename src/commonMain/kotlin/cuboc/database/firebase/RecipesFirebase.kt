package cuboc_core.cuboc.database.firebase

import cuboc.ingredient.Ingredient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import cuboc_core.cuboc.database.UserRecipe
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.serialization.Serializable
import utility.MeasureUnit
import utility.Name
import kotlin.random.Random

class RecipesFirebase(private val db: FirebaseFirestore) {
    private val collectionName = "recipes"

    @Serializable
    private data class RecipeInputFirebase(
        val name: String,
        val unit: String,
        val amount: Double,
        val scalable: Boolean
    ) {
        constructor(recipeInput: RecipeInput) : this(
            recipeInput.ingredient.name.toString(),
            recipeInput.ingredient.measureUnit.toString(),
            recipeInput.amount,
            recipeInput.scalable
        )

        fun toRecipeInput(): RecipeInput {
            return RecipeInput(
                Ingredient(Name(name), MeasureUnit(Name(unit))),
                amount,
                scalable
            )
        }
    }

    @Serializable
    private data class RecipeOutputFirebase(
        val name: String,
        val unit: String,
        val amount: Double,
        val scalable: Boolean
    ) {
        constructor(recipeOutput: RecipeOutput) : this(
            recipeOutput.ingredient.name.toString(),
            recipeOutput.ingredient.measureUnit.toString(),
            recipeOutput.amount,
            recipeOutput.scalable
        )

        fun toRecipeOutput(): RecipeOutput {
            return RecipeOutput(
                Ingredient(Name(name), MeasureUnit(Name(unit))),
                amount,
                scalable
            )
        }
    }

    private fun encodeRecipe(recipe: UserRecipe): Map<String, Any> {
        return mapOf(
            "name" to recipe.name.toString(),
            "inputs" to recipe.inputs.map { RecipeInputFirebase(it) },
            "outputs" to recipe.outputs.map { RecipeOutputFirebase(it) },
            "duration" to recipe.instruction.durationMinutes,
            "instructions" to recipe.instruction.text.toString(),
            "allInputNames" to recipe.inputs.map { it.ingredient.name.toString() }.toSet(),
            "allOutputNames" to recipe.outputs.map { it.ingredient.name.toString() }.toSet(),
            "searchableName" to recipe.name.toString().lowercase().trim(),
        )
    }

    private fun decodeRecipe(document: DocumentSnapshot): UserRecipe {
        return UserRecipe(
            document.id,
            Recipe(
                document.get("name"),
                document.get<List<RecipeInputFirebase>>("inputs").map { it.toRecipeInput() }.toSet(),
                document.get<List<RecipeOutputFirebase>>("outputs").map { it.toRecipeOutput() }.toSet(),
                Instruction(
                    document.get("duration"),
                    document.get("instructions")
                )
            )
        )
    }

    private fun generateRecipeId(recipeName: String): String {
        return recipeName + "_" + Random.nextLong().toString()
    }

    suspend fun put(recipe: Recipe): UserRecipe {
        val id = generateRecipeId(recipe.name.toString())
        val userRecipe = UserRecipe(id, recipe)
        db.collection(collectionName).document(id).set(encodeRecipe(userRecipe))
        return userRecipe
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
        return results.documents.map { decodeRecipe(it) }
    }

    suspend fun searchByInput(query: String): List<UserRecipe> {
        val results =
            db.collection(collectionName).where("allInputNames", arrayContains = query).get()
        return results.documents.map { decodeRecipe(it) }
    }

    suspend fun searchByOutput(query: String): List<UserRecipe> {
        val results =
            db.collection(collectionName).where("allOutputNames", arrayContains = query).get()
        return results.documents.map { decodeRecipe(it) }
    }
}
