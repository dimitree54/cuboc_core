package cuboc_core.cuboc.database.firebase

import cuboc.ingredient.Ingredient
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.serialization.Serializable
import utility.MeasureUnit
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
            recipeInput.ingredient.name,
            recipeInput.ingredient.measureUnit.name,
            recipeInput.amount,
            recipeInput.scalable
        )

        fun toRecipeInput(): RecipeInput {
            return RecipeInput(
                Ingredient(name, MeasureUnit(unit)),
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
            recipeOutput.ingredient.name,
            recipeOutput.ingredient.measureUnit.name,
            recipeOutput.amount,
            recipeOutput.scalable
        )

        fun toRecipeOutput(): RecipeOutput {
            return RecipeOutput(
                Ingredient(name, MeasureUnit(unit)),
                amount,
                scalable
            )
        }
    }

    private fun encodeRecipe(recipe: Recipe): Map<String, Any> {
        return mapOf(
            "name" to recipe.name,
            "inputs" to recipe.inputs.map { RecipeInputFirebase(it) },
            "outputs" to recipe.outputs.map { RecipeOutputFirebase(it) },
            "duration" to recipe.instruction.durationMinutes,
            "instructions" to recipe.instruction.text,
            "allInputNames" to recipe.inputs.map { it.ingredient.name }.toSet(),
            "allOutputNames" to recipe.outputs.map { it.ingredient.name }.toSet()
        )
    }

    private fun decodeRecipe(document: DocumentSnapshot): Recipe {
        return Recipe(
            document.id,
            document.get("name"),
            document.get<List<RecipeInputFirebase>>("inputs").map { it.toRecipeInput() }.toSet(),
            document.get<List<RecipeOutputFirebase>>("outputs").map { it.toRecipeOutput() }.toSet(),
            Instruction(
                document.get("duration"),
                document.get("instructions")
            )
        )
    }

    private fun generateRecipeId(recipeName: String): String {
        return recipeName + "_" + Random.nextLong().toString()
    }

    suspend fun put(newRecipe: Recipe): Recipe {
        if (newRecipe.id != null) {
            throw Exception("Recipe already exists")
        }
        val id = generateRecipeId(newRecipe.name)
        val recipe =
            Recipe(id, newRecipe.name, newRecipe.inputs, newRecipe.outputs, newRecipe.instruction)
        db.collection(collectionName).document(id).set(encodeRecipe(recipe))
        return recipe
    }

    suspend fun remove(recipe: Recipe): Boolean {
        if (recipe.id == null) {
            throw Exception("Recipe does not exist")
        }
        db.collection(collectionName).document(recipe.id).delete()
        return true
    }

    suspend fun searchByName(query: String): List<Recipe> {
        val results = db.collection(collectionName).where("name", query).get()
        return results.documents.map { decodeRecipe(it) }
    }

    suspend fun searchByInput(query: String): List<Recipe> {
        val results =
            db.collection(collectionName).where("allInputNames", arrayContains = query).get()
        return results.documents.map { decodeRecipe(it) }
    }

    suspend fun searchByOutput(query: String): List<Recipe> {
        val results =
            db.collection(collectionName).where("allOutputNames", arrayContains = query).get()
        return results.documents.map { decodeRecipe(it) }
    }
}