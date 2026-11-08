package com.example.scopeinspector.feature.inspector

import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

/** Lo que se sabe del portapapeles antes de leerlo y, si se pide, su contenido. */
sealed interface ClipboardReading {
    /** Solo la descripción (tipos MIME): Android no avisa al usuario de esta consulta. */
    data class Described(val hasClip: Boolean, val mimeTypes: List<String>) : ClipboardReading

    /** Contenido leído; desde Android 12 el sistema muestra un aviso «<app> ha pegado del portapapeles». */
    data class Read(val text: String?, val truncated: Boolean) : ClipboardReading
}

object ClipboardProbe {
    const val MAX_CHARS = 280

    fun describe(context: Context): ClipboardReading.Described {
        val clipboard = context.getSystemService<ClipboardManager>()
        val description = clipboard?.primaryClipDescription
        val mimeTypes = description?.let { d -> (0 until d.mimeTypeCount).map { d.getMimeType(it) } }.orEmpty()
        return ClipboardReading.Described(hasClip = clipboard?.hasPrimaryClip() == true, mimeTypes = mimeTypes)
    }

    /**
     * Lee el primer elemento sin abrir nada: si es un URI (`content://...`) se muestra el URI.
     * `ClipData.Item.coerceToText` leería entero el contenido del proveedor en el hilo de la interfaz,
     * sin límite de tamaño, y eso podría congelar la app con un fichero lento o enorme.
     */
    fun read(context: Context): ClipboardReading.Read {
        val item = context.getSystemService<ClipboardManager>()?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        val text = item?.text?.toString() ?: item?.htmlText ?: item?.uri?.toString() ?: item?.intent?.toUri(0)
        return truncate(text)
    }

    fun truncate(text: String?): ClipboardReading.Read = when {
        text == null -> ClipboardReading.Read(text = null, truncated = false)
        text.length > MAX_CHARS -> ClipboardReading.Read(text = text.take(MAX_CHARS) + "…", truncated = true)
        else -> ClipboardReading.Read(text = text, truncated = false)
    }
}
