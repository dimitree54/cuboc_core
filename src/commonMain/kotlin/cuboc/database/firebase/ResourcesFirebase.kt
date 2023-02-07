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

    private fun decodeReservations(document: DocumentSnapshot): Map<String, Double> {
        if (document.contains("reservations")) {
            return document.get("reservations")
        }
        return mapOf()
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

    suspend fun remove(resource: UserResource): Boolean {
        db.collection(collectionName).document(resource.id).delete()
        return true
    }

    suspend fun searchByName(query: String): List<UserResource> {
        val results = db.collection(collectionName).where("name", query).get()
        return results.documents.map { decodeResource(it) }
    }

    private fun getAvailableAmount(resource: Resource, reservations: Map<String, Double>): Double {
        return resource.amount - reservations.values.sum()
    }

    // only for admin
    suspend fun reserve(request: PieceOfResource, reserverId: String): Boolean {
        val id = request.resource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val resource = decodeResource(document)
        val reservations = decodeReservations(document)
        val availableAmount = getAvailableAmount(resource, reservations)
        return if (availableAmount >= request.amount) {
            val updatedReservations = reservations.toMutableMap().also {
                it[reserverId] = request.amount
            }
            documentReference.update("reservations" to updatedReservations)
            true
        } else {
            false
        }
    }

    // only for admin
    suspend fun release(request: PieceOfResource, reserverId: String): Boolean {
        val id = request.resource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val reservations = decodeReservations(document)
        val reservedAmount = reservations[reserverId] ?: return false
        return if (reservedAmount >= request.amount) {
            val updatedReservations = reservations.toMutableMap()
            if (reservedAmount == request.amount) {
                updatedReservations.remove(reserverId)
            } else {
                updatedReservations[reserverId] = reservedAmount - request.amount
            }
            documentReference.update("reservations" to updatedReservations)
            true
        } else {
            false
        }
    }

    // only for admin
    suspend fun getReservedAmount(request: PieceOfResource, reserverId: String): Boolean {
        val id = request.resource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val resource = decodeResource(document)
        if (release(request, reserverId)) {
            documentReference.update("amount" to resource.amount - request.amount)
            return true
        }
        return false
    }
}
