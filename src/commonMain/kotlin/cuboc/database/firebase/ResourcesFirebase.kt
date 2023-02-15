package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc_core.cuboc.database.search.SmartStringSearch
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where

open class ResourcesFirebase(protected val db: FirebaseFirestore, private val idGenerator: IdGenerator) {
    protected val collectionName = "resources"
    protected val resourceField = "resource"
    protected val smartSearchField = "searchableName"
    protected val reservationsField = "reservations"

    suspend fun put(resource: Resource): UserResource {
        val id = idGenerator.generateId(resource.ingredient.name.toString())
        val userResource = UserResource(id, resource)
        db.collection(collectionName).document(id).set(
            mapOf(
                resourceField to resource,
                smartSearchField to SmartStringSearch(resource.ingredient.name.toString()).normalisedQuery
            )
        )
        return userResource
    }

    suspend fun remove(resource: UserResource) {
        db.collection(collectionName).document(resource.id).delete()
    }

    protected fun decode(document: DocumentSnapshot): PieceOfUserResource {
        val resource = document.get<Resource>(resourceField)
        val reservations = decodeReservations(document)
        return PieceOfUserResource(UserResource(document.id, resource), getAvailableAmount(resource, reservations))
    }

    // todo refactor: get should return UserResource with reservations, not PieceOfUserResource
    suspend fun get(id: String): PieceOfUserResource {
        val document = db.collection(collectionName).document(id).get()
        return decode(document)
    }

    suspend fun searchByName(query: String): List<PieceOfUserResource> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(collectionName)
            .where(smartSearchField, equalTo = smartSearch.normalisedQuery).get()
        return results.documents.map(::decode)
    }

    suspend fun smartSearchByName(query: String): List<PieceOfUserResource> {
        val smartSearch = SmartStringSearch(query)
        val results = db.collection(collectionName)
            .where(smartSearchField, greaterThan = smartSearch.searchBoundaries.first)
            .where(smartSearchField, lessThan = smartSearch.searchBoundaries.second).get()
        return results.documents.map(::decode)
    }

    private fun getAvailableAmount(resource: Resource, reservations: Map<String, Double>): Double {
        return resource.amount - reservations.values.sum()
    }

    protected fun decodeReservations(document: DocumentSnapshot): Map<String, Double> {
        if (document.contains(reservationsField)) {
            return document.get(reservationsField)
        }
        return mapOf()
    }
}

