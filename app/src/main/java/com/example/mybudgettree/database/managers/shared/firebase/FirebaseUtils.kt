package com.example.mybudgettree.database.managers.shared.firebase

import com.example.mybudgettree.database.managers.shared.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

data class RelatedCollection(
    val collection: CollectionReference,
    val referenceField: String
)

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
