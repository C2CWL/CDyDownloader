package c2cw.cdydownload

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap

/** Writing into, labelling and cleaning up folders picked with ACTION_OPEN_DOCUMENT_TREE. */
internal object SaveTree {
    fun label(tree: String): String? = runCatching {
        treeDocumentLabel(DocumentsContract.getTreeDocumentId(Uri.parse(tree)))
    }.getOrNull()

    fun hasWriteAccess(resolver: ContentResolver, tree: String): Boolean {
        val uri = Uri.parse(tree)
        return resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
    }

    fun persist(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    /** Gives back the old grant if no setting uses it anymore; there is a per-app limit on grants. */
    fun releaseUnused(context: Context, old: String, stillUsed: Collection<String>) {
        if(old.isBlank() || old in stillUsed) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(old),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    /**
     * Creates an empty document. The provider may rewrite the extension when it does not match the
     * MIME type (for example .m4a vs audio/mp4), so pass the type the platform associates with it.
     */
    fun create(resolver: ContentResolver, tree: String, mime: String, displayName: String): Uri {
        if(!hasWriteAccess(resolver, tree)) error(SAVE_TREE_LOST)
        val treeUri = Uri.parse(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        val type = when {
            mapped == null -> "application/octet-stream"
            mapped.equals(mime, true) -> mime
            else -> mapped
        }
        val created: Uri? = try {
            DocumentsContract.createDocument(resolver, parent, type, displayName)
        } catch(e: SecurityException) { null } catch(e: IllegalArgumentException) { null }
            catch(e: java.io.FileNotFoundException) { null }
        return created ?: error(SAVE_TREE_LOST)
    }

    fun isDocument(context: Context, uri: Uri) = runCatching { DocumentsContract.isDocumentUri(context, uri) }.getOrDefault(false)
}

/** Deletes a saved output, whether it lives in MediaStore or in a picked folder. */
internal fun deleteSavedOutput(context: Context, value: String) {
    val uri = Uri.parse(value)
    if(SaveTree.isDocument(context, uri)) {
        try {
            if(!DocumentsContract.deleteDocument(context.contentResolver, uri)) error("文件无法删除")
        } catch(_: java.io.FileNotFoundException) { /* already gone */ }
    } else context.contentResolver.delete(uri, null, null)
}
