package com.example.scopeinspector.feature.inspector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardProbeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun copy(text: String) {
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(ClipData.newPlainText("test", text))
    }

    @Test
    fun anEmptyClipboardHasNothingToDescribeOrRead() {
        assertFalse(ClipboardProbe.describe(context).hasClip)
        assertNull(ClipboardProbe.read(context).text)
    }

    @Test
    fun theDescriptionGivesTheTypeWithoutTheContent() {
        copy("1234 5678")

        val described = ClipboardProbe.describe(context)

        assertTrue(described.hasClip)
        assertEquals(listOf("text/plain"), described.mimeTypes)
    }

    @Test
    fun readingReturnsTheCopiedText() {
        copy("contraseña123")

        assertEquals(ClipboardReading.Read("contraseña123", truncated = false), ClipboardProbe.read(context))
    }

    @Test
    fun longTextIsTruncated() {
        val read = ClipboardProbe.truncate("a".repeat(ClipboardProbe.MAX_CHARS + 10))

        assertTrue(read.truncated)
        assertEquals(ClipboardProbe.MAX_CHARS + 1, read.text!!.length)
        assertFalse(ClipboardProbe.truncate("a".repeat(ClipboardProbe.MAX_CHARS)).truncated)
    }
}
