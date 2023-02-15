package cuboc.database.firebase

import cuboc.ingredient.PieceOfUserResource
import cuboc_core.utility.IdGenerator
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ResourcesFirebaseAdmin(db: FirebaseFirestore, idGenerator: IdGenerator) : ResourcesFirebase(db, idGenerator) {
    suspend fun reserve(request: PieceOfUserResource, reserverId: String) {
        val documentReference = db.collection(collectionName).document(request.userResource.id)
        val document = documentReference.get()
        val pieceOfUserResource = decode(document)
        if (pieceOfUserResource.amount >= request.amount) {
            documentReference.update(reservationsField to FieldValue.arrayUnion(reserverId to request.amount))
        } else {
            throw Exception("Not enough of resource to reserve")
        }
    }

    private suspend fun release(
        request: PieceOfUserResource,
        documentReference: DocumentReference,
        document: DocumentSnapshot,
        reserverId: String
    ) {
        val reservations = decodeReservations(document)
        val reservedAmount = reservations[reserverId] ?: throw Exception("Can not release not reserved resource")
        if (reservedAmount >= request.amount) {
            val amountStillReserved = reservedAmount - request.amount
            db.runTransaction {
                documentReference.update(reservationsField to FieldValue.arrayRemove(reserverId))
                if (amountStillReserved > 0) {
                    documentReference.update(reservationsField to FieldValue.arrayUnion(reserverId to amountStillReserved))
                }
            }
        } else {
            throw Exception("You reserved less resources than trying to release")
        }
    }

    suspend fun release(request: PieceOfUserResource, reserverId: String) {
        val documentReference = db.collection(collectionName).document(request.userResource.id)
        val document = documentReference.get()
        release(request, documentReference, document, reserverId)
    }

    suspend fun consumeReserved(request: PieceOfUserResource, reserverId: String) {
        val id = request.userResource.id
        val documentReference = db.collection(collectionName).document(id)
        val document = documentReference.get()
        val pieceOfUserResource = decode(document)
        db.runTransaction {
            release(request, documentReference, document, reserverId)
            if (isLastConsumed(document, request, reserverId)) {
                documentReference.delete()
            } else {
                documentReference.update(reserverId to pieceOfUserResource.resource.amount - request.amount)
            }
        }
    }

    private fun isLastConsumed(
        documentBeforeRelease: DocumentSnapshot,
        consumed: PieceOfUserResource,
        reserverId: String
    ): Boolean {
        val reservations = decodeReservations(documentBeforeRelease)
        return (reservations.size == 1 && reservations[reserverId] == consumed.amount)
    }
}