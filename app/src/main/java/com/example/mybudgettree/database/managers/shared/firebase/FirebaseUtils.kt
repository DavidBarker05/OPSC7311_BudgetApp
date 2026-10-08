package com.example.mybudgettree.database.managers.shared.firebase

import com.example.mybudgettree.database.managers.shared.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

/**
 * Describes a collection whose documents point at another document by ID, and so must be deleted along with it.
 * Firestore has no cascading deletes, so these have to be removed by hand (see [deleteDocument])
 *
 * @property collection The collection holding the documents that point at the deleted one (e.g. the user's expenses)
 * @property referenceField The field in those documents that holds the deleted document's ID (e.g. "categoryId")
 */
data class RelatedCollection(
    val collection: CollectionReference,
    val referenceField: String
)

/**
 * Updates one field of a document and reports the result, so the managers don't each repeat the same checks and
 * error handling
 *
 * The document's ID is found by reflection: the entity's `id` property, otherwise its `uid`, otherwise the
 * `DOCUMENT_ID` constant in its companion object (e.g. [com.example.mybudgettree.database.entries.UserTree.DOCUMENT_ID]).
 * The field written is named after [property], so a property must have the same name as its stored field
 *
 * @param T The kind of entity being updated
 * @param V The type of the field being updated
 * @param collection The collection the document is in
 * @param entityTypeDisplayName A readable name for the kind of entity being updated, used in errors
 * @param entity The entity being updated, as it currently is
 * @param property The property of [entity] to update (e.g. `Category::categoryName`)
 * @param newValue The value to write to the field
 * @param updatedEntity The entity with [newValue] applied, returned when the update succeeds
 * @return An [UpdateReturnInfo]: [UpdateReturnStatus.NoChange] if the field already holds [newValue],
 * [UpdateReturnStatus.Succeeded] with [updatedEntity] if it was written, or [UpdateReturnStatus.Failed] if the entity
 * has no usable ID, the document no longer exists, or the write failed
 */
suspend fun <T: Any, V> updateDocumentField(
    collection: CollectionReference,
    entityTypeDisplayName: String,
    entity: T,
    property: KProperty1<T, V>,
    newValue: V,
    updatedEntity: T
): UpdateReturnInfo<T> {
    val idProp =
        entity::class.memberProperties.find { it.name == "id" }
        ?: entity::class.memberProperties.find { it.name == "uid" }
    val id = (
            if (idProp != null) idProp.getter.call(entity) as? String
            else run {
                val companionClass = entity::class.companionObject ?: return@run null
                val companionInstance = entity::class.companionObjectInstance ?: return@run null
                val idConstProp = companionClass.memberProperties.find { it.name == "DOCUMENT_ID" } ?: return@run null
                return@run idConstProp.getter.call(companionInstance) as? String
            }
            )
        ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName does not have a uid, id or DOCUMENT_ID")
    if (id.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName id is empty")
    if (property.get(entity) == newValue) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = entity)
    return try {
        collection.document(id).update(property.name, newValue).await()
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
 * Sets or removes the local image path one device saved for a document, leaving every other device's path alone.
 * The paths are kept in an `imagePaths` map field (deviceId -> local path), and only the one entry is written, so
 * two devices saving at the same time can't overwrite each other
 *
 * @param collection The collection the document is in
 * @param entityTypeDisplayName A readable name for the kind of entity being updated, used in errors
 * @param entity The entity being updated
 * @param id The ID of the document to update
 * @param currentPaths The entity's current `imagePaths`
 * @param deviceId The ID of the device the image is saved on
 * @param newPath The new local image path, or null to remove this device's image
 * @param withPaths Builds the updated entity from the new `imagePaths`
 * @return An [UpdateReturnInfo] indicating what happened with the update
 */
suspend fun <T : Any> updateDeviceImagePath(
    collection: CollectionReference,
    entityTypeDisplayName: String,
    entity: T,
    id: String,
    currentPaths: Map<String, String>,
    deviceId: String,
    newPath: String?,
    withPaths: (Map<String, String>) -> T
): UpdateReturnInfo<T> {
    if (id.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName id is empty")
    if (deviceId.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Device id is empty")
    if (newPath?.isBlank() == true) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Image path cannot be blank")
    if (currentPaths[deviceId] == newPath) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = entity)
    return try {
        // FieldPath.of avoids the device ID being parsed as part of a dotted path
        collection.document(id).update(FieldPath.of("imagePaths", deviceId), newPath ?: FieldValue.delete()).await()
        val updatedPaths = if (newPath == null) currentPaths - deviceId else currentPaths + (deviceId to newPath)
        UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = withPaths(updatedPaths))
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "$entityTypeDisplayName does not exist")
        else UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update image")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update image")
    }
}

/**
 * Deletes every document a query matches, a batch at a time, since Firestore can't delete a collection in one call.
 * Pass a whole collection to empty it, or a filtered query to delete only the documents that match
 *
 * @param db The Firestore instance holding the documents
 * @param query The documents to delete. A [CollectionReference] is a query that matches everything in it
 * @param batchSize How many documents to delete per batch, at most 500 and above 0 (a batch size of 0 deletes nothing)
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
 * Deletes a document along with everything that belongs to it
 *
 * Firestore has no cascading deletes, so everything that belongs to the document is removed by hand first:
 * - each collection in [subcollections], since deleting a document does not delete its subcollections
 * - every document in the [relatedCollections] that points at it by ID, such as the expenses and incomes of a category
 *
 * These are cleared before the document itself so that, if something fails part way, the document still exists and
 * the delete can simply be tried again. Failures are not caught here, so they propagate to the caller
 *
 * @param db The Firestore instance holding the document
 * @param collection The collection the document is in
 * @param id The ID of the document to delete
 * @param subcollections The collections stored under the document that must be deleted with it
 * @param relatedCollections The other collections whose documents point at this one by ID, and the field they use
 * @param batchSize How many documents to delete per batch when clearing the other collections, at most 500 and above 0
 * @return [DeleteReturnStatus.DoesNotExist] if the ID is blank or the document isn't there, otherwise [DeleteReturnStatus.Deleted]
 */
suspend fun deleteDocument(
    db: FirebaseFirestore,
    collection: CollectionReference,
    id: String,
    subcollections: List<CollectionReference>,
    relatedCollections: List<RelatedCollection>,
    batchSize: Long
): DeleteReturnStatus {
    if (id.isBlank()) return DeleteReturnStatus.DoesNotExist
    val documentRef = collection.document(id)
    if (!documentRef.get().await().exists())  return DeleteReturnStatus.DoesNotExist
    for (subcollection in subcollections) deleteCollection(db, subcollection, batchSize)
    for (related in relatedCollections) {
        val pointingAtDocument = related.collection.whereEqualTo(related.referenceField, id)
        deleteCollection(db, pointingAtDocument, batchSize)
    }
    documentRef.delete().await()
    return DeleteReturnStatus.Deleted
}
