package com.example.scopeinspector.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.BuildConfig
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VersionFooterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheVersionNameAndTheBuildNumber() {
        composeRule.setContent { ScopeInspectorTheme { VersionFooter() } }

        composeRule
            .onNodeWithText("v${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}")
            .assertIsDisplayed()
    }
}
