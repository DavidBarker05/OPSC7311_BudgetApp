package com.example.mybudgettree.database.managers.shared.firebase

import com.example.mybudgettree.database.managers.shared.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KProperty1

/**
 * Describes another collection under `users/{uid}` whose documents point at a document by ID, and so must be
 * deleted along with it. Firestore has no cascading deletes, so these have to be removed by hand
 *
 * @property collectionName The name of the collection holding the documents that point at the deleted one
 * @property referenceField The field in those documents that holds the deleted document's ID (e.g. "categoryId")
 */
data class RelatedCollection(
    val collectionName: String,
    val referenceField: String
)

private fun collection(
    db: FirebaseFirestore,
    uid: String,
    collectionName: String
): CollectionReference =
    db.collection("users")
        .document(uid)
        .collection(collectionName)

@Suppress("UNCHECKED_CAST")
suspend fun <T: Any, V> updateDocumentField(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    collectionName: String,
    entityTypeDisplayName: String,
    id: String,
    entity: T,
    property: KProperty1<T, V>,
    value: V,
    updatedEntity: T,
): UpdateReturnInfo<T> {
    if (id.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName id is empty")
    val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
    if (property.get(entity) == value) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = entity)
    return try {
        collection(db, uid, collectionName).document(id).update(property.name, value).await()
        UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = updatedEntity)
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName does not exist")
        else UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update ${property.name}")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update ${property.name}")
    }
}

/**
 * Deletes every document a query matches, a batch at a time, since Firestore can't delete a collection in one call.
 * Pass a whole collection to empty it, or a filtered query to delete only the documents that match
 *
 * @param db The Firestore instance holding the documents
 * @param query The documents to delete. A [CollectionReference] is a query that matches everything in it
 * @param batchSize How many documents to delete per batch, at most 500
 */
private suspend fun deleteCollection(
    db: FirebaseFirestore,
    query: Query,
    batchSize: Long
) {
    while (true) {
        val documents = query.limit(batchSize).get().await().documents
        if (documents.isEmpty()) return
        db.runBatch { batch -> documents.forEach { batch.delete(it.reference) } }.await()
    }
}

/**
 * Deletes one of the signed-in user's documents along with everything that belongs to it
 *
 * Firestore has no cascading deletes, so everything that belongs to the document is removed by hand first:
 * - each collection stored beneath it in [subCollections], since deleting a document does not delete its
 *   subcollections
 * - every document in the [relatedCollections] that points at it by ID, such as the expenses and incomes of a
 *   category
 *
 * These are cleared before the document itself so that, if something fails part way, the document still exists
 * and the delete can simply be tried again. Network failures are not caught here, so they propagate to the caller
 *
 * @param auth The Firebase Authentication instance, used to find the signed-in user
 * @param db The Firestore instance holding the document
 * @param collectionName The name of the collection under `users/{uid}` the document is in
 * @param id The ID of the document to delete
 * @param subCollections The names of the collections stored under the document that must be deleted with it
 * @param relatedCollections The other collections whose documents point at this one by ID, and the field they use
 * @param batchSize How many documents to delete per batch when clearing the subcollections, at most 500
 * @return A [DeleteReturnStatus] describing what happened. [DeleteReturnStatus.ReauthenticationFailed] is returned
 * when nobody is signed in
 */
suspend fun deleteDocument(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    collectionName: String,
    id: String,
    subCollections: List<String>,
    relatedCollections: List<RelatedCollection>,
    batchSize: Long
): DeleteReturnStatus {
    if (id.isBlank()) return DeleteReturnStatus.DoesNotExist
    val uid = auth.uid ?: return DeleteReturnStatus.ReauthenticationFailed
    val documentRef = collection(db, uid, collectionName).document(id)
    if (!documentRef.get().await().exists())  return DeleteReturnStatus.DoesNotExist
    for (name in subCollections) deleteCollection(db, documentRef.collection(name), batchSize)
    for (related in relatedCollections) {
        val pointingAtDocument = collection(db, uid, related.collectionName).whereEqualTo(related.referenceField, id)
        deleteCollection(db, pointingAtDocument, batchSize)
    }
    documentRef.delete().await()
    return DeleteReturnStatus.Deleted
}
