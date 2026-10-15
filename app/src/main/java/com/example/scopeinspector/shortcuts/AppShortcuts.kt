package com.example.scopeinspector.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.example.scopeinspector.MainActivity
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule

/** Accesos directos de la app: atajos a los módulos y acceso directo en la pantalla de inicio. */
object AppShortcuts {
    private const val PREFS = "shortcuts"
    private const val KEY_PIN_OFFERED = "home_pin_offered"

    /**
     * Atajos que salen al mantener pulsado el icono de la app, uno por módulo. Se recalculan en
     * cada arranque (es idempotente) a partir de [DemoModule], que es la única fuente de verdad.
     */
    fun registerModuleShortcuts(context: Context) {
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val shortcuts = DemoModule.entries.take(max).mapIndexed { index, module ->
            ShortcutInfoCompat.Builder(context, ShortcutPolicy.moduleShortcutId(module.route))
                .setShortLabel(context.getString(module.shortLabelRes))
                .setLongLabel(context.getString(module.titleRes))
                .setIcon(IconCompat.createWithResource(context, module.shortcutIconRes))
                .setIntent(
                    Intent(
                        Intent.ACTION_VIEW,
                        ShortcutPolicy.moduleUri(module.route).toUri(),
                        context,
                        MainActivity::class.java,
                    ),
                )
                .setRank(index)
                .build()
        }
        // Un fallo al publicar atajos no debe impedir que la app arranque.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    /**
     * Pide a Android que añada el icono de la app a la pantalla de inicio. Solo se ofrece una vez
     * (aunque el usuario rechace) y el sistema muestra su propio diálogo de confirmación.
     */
    fun offerHomeShortcut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alreadyPinned = runCatching {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .any { it.id == ShortcutPolicy.HOME_SHORTCUT_ID }
        }.getOrDefault(false)
        val offer = ShortcutPolicy.shouldOfferPin(
            supported = ShortcutManagerCompat.isRequestPinShortcutSupported(context),
            alreadyOffered = prefs.getBoolean(KEY_PIN_OFFERED, false),
            alreadyPinned = alreadyPinned,
        )
        if (!offer) return

        // Se marca antes de pedirlo: si el usuario rechaza, no se le vuelve a preguntar.
        prefs.edit { putBoolean(KEY_PIN_OFFERED, true) }
        val shortcut = ShortcutInfoCompat.Builder(context, ShortcutPolicy.HOME_SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.app_name))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        runCatching { ShortcutManagerCompat.requestPinShortcut(context, shortcut, null) }
    }
}
