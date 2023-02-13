package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.Resource
import cuboc.ingredient.UserResource
import cuboc_core.utility.IdGenerator
import cuboc_core.utility.Report
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where

open class ResourcesFirebase(protected val db: FirebaseFirestore, private val idGenerator: IdGenerator) {
    protected val collectionName = "resources"

    suspend fun put(resource: Resource): UserResource {
        val id = idGenerator.generateId(resource.ingredient.name.toString())
        val userResource = UserResource(id, resource)
        db.collection(collectionName).document(id).set(
            "resource" to resource
        )
        return userResource
    }

    suspend fun remove(resource: UserResource): Boolean {
        db.collection(collectionName).document(resource.id).delete()
        return true
    }

    protected fun decode(document: DocumentSnapshot): PieceOfUserResource {
        val resource = document.get<Resource>("resource")
        val reservations = decodeReservations(document)
        return PieceOfUserResource(UserResource(document.id, resource), getAvailableAmount(resource, reservations))
    }

    suspend fun get(id: String): PieceOfUserResource {
        val document = db.collection(collectionName).document(id).get()
        return decode(document)
    }

    suspend fun searchByName(query: String): List<PieceOfUserResource> {
        val searchQuery = query.lowercase().trim()
        val lastChar = searchQuery.last()
        val smartSearchGreaterThan = searchQuery.dropLast(1) + (lastChar - 1)
        val smartSearchLessThan = searchQuery.dropLast(1) + (lastChar + 1)
        val results = db.collection(collectionName).where("searchableName", greaterThan = smartSearchGreaterThan)
            .where("searchableName", lessThan = smartSearchLessThan).get()
        return results.documents.map(::decode)
    }

    suspend fun searchByAuthor(authorId: String): List<PieceOfUserResource> {
        TODO()
    }

    private fun getAvailableAmount(resource: Resource, reservations: Map<String, Double>): Double {
        return resource.amount - reservations.values.sum()
    }

    protected fun decodeReservations(document: DocumentSnapshot): Map<String, Double> {
        if (document.contains("reservations")) {
            return document.get("reservations")
        }
        return mapOf()
    }

    private fun decodeReports(document: DocumentSnapshot): List<Report> {
        if (document.contains("reports")) {
            return document.get("reports")
        }
        return emptyList()
    }

    suspend fun report(resourceToReport: UserResource, report: Report): Boolean {
        val id = resourceToReport.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val reports = decodeReports(document)
        documentReference.update("reports" to reports + report)
        return true
    }
}

