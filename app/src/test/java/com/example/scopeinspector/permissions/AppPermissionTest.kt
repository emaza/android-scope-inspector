package com.example.scopeinspector.permissions

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
    fun manifestNamesAreUnique() {
        val names = AppPermission.entries.map { it.manifestName }
        assertEquals(names.size, names.toSet().size)
    }
}
