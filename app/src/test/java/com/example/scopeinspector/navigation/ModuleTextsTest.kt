package com.example.scopeinspector.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.R
import com.example.scopeinspector.permissions.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba los textos reales (no los ids): que existan, que no estén vacíos y que no se repitan. */
@RunWith(AndroidJUnit4::class)
class ModuleTextsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun moduleTexts(module: DemoModule): Map<String, String> = mapOf(
        "title" to string(module.titleRes),
        "shortLabel" to string(module.shortLabelRes),
        "summary" to string(module.summaryRes),
        "feature" to string(module.featureRes),
        "example" to string(module.exampleRes),
        "howToTest" to string(module.howToTestRes),
        "rationale" to string(module.rationaleRes),
    )

    @Test
    fun everyModuleTextIsNotBlank() {
        DemoModule.entries.forEach { module ->
            moduleTexts(module).forEach { (field, text) ->
                assertTrue("${module.name}.$field está vacío", text.isNotBlank())
            }
        }
    }

    @Test
    fun noDescriptiveTextIsRepeatedAcrossModulesAndFields() {
        // El título y la etiqueta corta pueden coincidir (p. ej. "Biometría"); los textos descriptivos no.
        val descriptive = listOf("summary", "feature", "example", "howToTest", "rationale")
        val all = DemoModule.entries.flatMap { module -> descriptive.map { moduleTexts(module).getValue(it) } }
        assertEquals("hay textos duplicados", all.size, all.toSet().size)
    }

    @Test
    fun moduleTitlesAreUnique() {
        val titles = DemoModule.entries.map { string(it.titleRes) }
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun shortcutLabelsAreUnique() {
        val labels = DemoModule.entries.map { string(it.shortLabelRes) }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun shortcutLabelsFitTheLauncherRecommendation() {
        // Los lanzadores recomiendan etiquetas cortas (unos 10 caracteres) para los atajos.
        DemoModule.entries.forEach { module ->
            val label = string(module.shortLabelRes)
            assertTrue("'$label' tiene ${label.length} caracteres", label.length <= 10)
        }
    }

    @Test
    fun everyPermissionHasANonBlankLabel() {
        AppPermission.entries.forEach { permission ->
            assertTrue(permission.name, string(permission.labelRes).isNotBlank())
        }
    }

    @Test
    fun theVersionLabelFormatsTheVersionAndTheBuild() {
        assertEquals("v1.2.3 · build 42", string(R.string.version_label, "1.2.3", 42))
    }

    @Test
    fun theAccuracyLabelFormatsMeters() {
        assertEquals("±12 m", string(R.string.accuracy_value, 12))
    }

    @Test
    fun theStateLabelsAreAllDifferent() {
        val labels = listOf(
            R.string.state_granted,
            R.string.state_granted_at_install,
            R.string.state_denied,
            R.string.state_permanently_denied,
            R.string.state_not_applicable,
        ).map { string(it) }
        assertEquals(labels.size, labels.toSet().size)
        assertFalse(labels.any { it.isBlank() })
    }
}
