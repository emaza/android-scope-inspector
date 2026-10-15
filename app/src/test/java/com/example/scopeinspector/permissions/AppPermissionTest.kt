package com.example.scopeinspector.permissions

import com.example.scopeinspector.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPermissionTest {

    @Test
    fun activityRecognitionOnlyAppliesFromAndroid10() {
        assertFalse(AppPermission.ActivityRecognition.isApplicable(sdkInt = 28))
        assertTrue(AppPermission.ActivityRecognition.isApplicable(sdkInt = 29))
    }

    @Test
    fun postNotificationsOnlyAppliesFromAndroid13() {
        assertFalse(AppPermission.PostNotifications.isApplicable(sdkInt = 32))
        assertTrue(AppPermission.PostNotifications.isApplicable(sdkInt = 33))
    }

    @Test
    fun useBiometricOnlyAppliesFromAndroid9() {
        assertFalse(AppPermission.UseBiometric.isApplicable(sdkInt = 27))
        assertTrue(AppPermission.UseBiometric.isApplicable(sdkInt = 28))
    }

    @Test
    fun cameraAndLocationApplyOnEveryVersion() {
        val minSdk = 26
        assertTrue(AppPermission.Camera.isApplicable(minSdk))
        assertTrue(AppPermission.FineLocation.isApplicable(minSdk))
    }

    @Test
    fun onlyUseBiometricIsGrantedAtInstall() {
        val installTime = AppPermission.entries.filter { it.grantedAtInstall }
        assertEquals(listOf(AppPermission.UseBiometric), installTime)
    }

    @Test
    fun grantedInstallTimePermissionShowsGrantedAtInstallLabel() {
        assertEquals(
            R.string.state_granted_at_install,
            AppPermission.UseBiometric.stateLabelRes(PermissionState.Granted),
        )
        assertEquals(
            R.string.state_not_applicable,
            AppPermission.UseBiometric.stateLabelRes(PermissionState.NotApplicable),
        )
    }

    @Test
    fun grantedRuntimePermissionKeepsPlainGrantedLabel() {
        assertEquals(R.string.state_granted, AppPermission.Camera.stateLabelRes(PermissionState.Granted))
    }

    @Test
    fun manifestNamesAreUnique() {
        val names = AppPermission.entries.map { it.manifestName }
        assertEquals(names.size, names.toSet().size)
    }
}
