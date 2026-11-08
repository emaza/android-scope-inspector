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

    fun read(context: Context): ClipboardReading.Read {
        val clip = context.getSystemService<ClipboardManager>()?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        return truncate(text)
    }

    fun truncate(text: String?): ClipboardReading.Read = when {
        text == null -> ClipboardReading.Read(text = null, truncated = false)
        text.length > MAX_CHARS -> ClipboardReading.Read(text = text.take(MAX_CHARS) + "…", truncated = true)
        else -> ClipboardReading.Read(text = text, truncated = false)
    }
}
