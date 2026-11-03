package com.avioiva.financiiea.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

/** Sharing exported files (PDF/CSV) and saving backups to the phone. */
object ShareUtils {

    /** Opens the Android share sheet for a generated file. */
    fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "com.avioiva.financiiea.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    /** Saves a text file (e.g. CSV or JSON backup) into the phone's Downloads folder. */
    fun saveToDownloads(context: Context, fileName: String, mime: String, content: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: return false
                true
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, fileName).writeText(content, Charsets.UTF_8)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Saves a text file into the connected Drive folder (SAF tree). Returns null on failure. */
    fun saveToTree(context: Context, treeUri: Uri, fileName: String, mime: String, content: String): Boolean {
        return try {
            val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            val doc = folder.createFile(mime, fileName) ?: return false
            context.contentResolver.openOutputStream(doc.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Writes a text file into the app's private exports dir (used for sharing). */
    fun writeCacheFile(context: Context, fileName: String, content: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
        return file
    }
}
