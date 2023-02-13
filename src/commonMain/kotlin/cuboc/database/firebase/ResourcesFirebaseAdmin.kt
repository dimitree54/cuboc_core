package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ResourcesFirebaseAdmin(db: FirebaseFirestore, idGenerator: IdGenerator) : ResourcesFirebase(db, idGenerator) {
    suspend fun reserve(request: PieceOfUserResource, reserverId: String): Boolean {
        val id = request.userResource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val pieceOfUserResource = decode(document)
        val reservations = decodeReservations(document)
        return if (pieceOfUserResource.amount >= request.amount) {
            documentReference.update("reservations" to reservations + (reserverId to request.amount))
            true
        } else {
            false
        }
    }

    suspend fun release(request: PieceOfUserResource, reserverId: String): Boolean {
        val id = request.userResource.id
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

    suspend fun consumeReserved(request: PieceOfUserResource, reserverId: String): Boolean {
        val id = request.userResource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val pieceOfUserResource = decode(document)
        if (release(request, reserverId)) {
            documentReference.update("amount" to pieceOfUserResource.resource.amount - request.amount)
            deleteIfEmpty(id)
            return true
        }
        return false
    }

    private suspend fun deleteIfEmpty(id: String): Boolean {
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val pieceOfUserResource = decode(document)
        val reservations = decodeReservations(document)
        if (reservations.isEmpty() && pieceOfUserResource.userResource.amount == 0.0) {
            documentReference.delete()
            return true
        }
        return false
    }
}