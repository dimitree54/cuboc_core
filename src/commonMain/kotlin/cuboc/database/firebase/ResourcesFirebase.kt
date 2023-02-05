package cuboc_core.cuboc.database.firebase

import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
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

    suspend fun get(request: PieceOfResource): PieceOfResource? {
        val id = request.resource.id ?: throw Exception("Resource does not exist")
        val document = db.collection(collectionName).document(id)
        val resource = decodeResource(document.get())
        return if (resource.amount >= request.amount) {
            document.set(
                mapOf(
                    "amount" to resource.amount - request.amount
                )
            )
            request
        } else if (resource.amount == request.amount) {
            document.delete()
            request
        } else {
            throw Exception("Not enough resources")
        }
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