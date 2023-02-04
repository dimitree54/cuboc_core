package cuboc_core.cuboc.database.firebase

import cuboc.database.CUBOCDatabase
import cuboc.ingredient.*
import cuboc.recipe.Instruction
import cuboc.recipe.Recipe
import cuboc.recipe.Scenario
import cuboc_core.cuboc.database.search.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.serialization.Serializable
import utility.MeasureUnit
import kotlin.random.Random

class ResourcesFirebase(private val db: FirebaseFirestore) {
    private val collectionName = "resources"

    private fun generateResourceId(ingredient_name: String): String {
        return ingredient_name + "_" + Random.nextLong().toString()
    }

    private fun encodeResource(resource: Resource): Map<String, Any> {
        return mapOf(
            "name" to resource.ingredient.name,
            "unit" to resource.ingredient.measureUnit.name,
            "amount" to resource.amount
        )
    }

    private fun decodeResource(document: DocumentSnapshot): Resource {
        return Resource(
            document.id,
            Ingredient(
                document.get("name"),
                MeasureUnit(document.get("unit"))
            ),
            document.get("amount")
        )
    }

    suspend fun put(newResource: Resource): Resource {
        if (newResource.id != null) {
            throw Exception("Resource already exists")
        }
        val id = generateResourceId(newResource.ingredient.name)
        val resource = Resource(id, newResource.ingredient, newResource.amount)
        db.collection(collectionName).document(id).set(encodeResource(resource))
        return resource
    }

    suspend fun remove(resource: Resource): Boolean {
        if (resource.id == null) {
            throw Exception("Resource does not exist")
        }
        db.collection(collectionName).document(resource.id).delete()
        return true
    }

    suspend fun searchByName(query: String): List<Resource> {
        val results = db.collection(collectionName).where("name", query).get()
        return results.documents.map { decodeResource(it) }
    }
}

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

class CUBOCFirebase : CUBOCDatabase {
    private val db = Firebase.firestore
    private val resourcesDatabase = ResourcesFirebase(db)
    private val recipesDatabase = RecipesFirebase(db)
    override suspend fun execute(scenario: Scenario): PieceOfResource? {
        TODO("Not yet implemented")
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

    override suspend fun removeResource(resource: Resource): Boolean {
        return resourcesDatabase.remove(resource)
    }

    override suspend fun removeRecipe(recipe: Recipe): Boolean {
        return recipesDatabase.remove(recipe)
    }

    override suspend fun addResource(resource: Resource): Resource {
        return resourcesDatabase.put(resource)
    }

    override suspend fun addRecipe(recipe: Recipe): Recipe {
        return recipesDatabase.put(recipe)
    }
}
