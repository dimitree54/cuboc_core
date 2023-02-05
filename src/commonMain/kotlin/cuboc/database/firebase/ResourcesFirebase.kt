package cuboc_core.cuboc.database.firebase

import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource
import cuboc_core.cuboc.database.UserResource
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import utility.MeasureUnit
import kotlin.random.Random

class ResourcesFirebase(private val db: FirebaseFirestore) {
    private val collectionName = "resources"

    private fun generateResourceId(name: String): String {
        return name + "_" + Random.nextLong().toString()
    }

    private fun encodeResource(resource: UserResource): Map<String, Any> {
        return mapOf(
            "name" to resource.ingredient.name,
            "unit" to resource.ingredient.measureUnit.name,
            "amount" to resource.amount
        )
    }

    private fun decodeResource(document: DocumentSnapshot): UserResource {
        return UserResource(
            document.id,
            Resource(
                Ingredient(
                    document.get("name"),
                    MeasureUnit(document.get("unit"))
                ),
                document.get("amount")
            )
        )
    }

    suspend fun put(resource: Resource): UserResource {
        val id = generateResourceId(resource.ingredient.name)
        val userResource = UserResource(id, resource)
        db.collection(collectionName).document(id).set(encodeResource(userResource))
        return userResource
    }

    suspend fun get(request: PieceOfResource): PieceOfResource? {
        val id = request.resource.id
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
            return null
        }
    }

    suspend fun remove(resource: UserResource): Boolean {
        db.collection(collectionName).document(resource.id).delete()
        return true
    }

    suspend fun searchByName(query: String): List<UserResource> {
        val results = db.collection(collectionName).where("name", query).get()
        return results.documents.map { decodeResource(it) }
    }
}