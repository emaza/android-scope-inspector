package com.example.scopeinspector.permissions

import com.example.scopeinspector.permissions.AppPermission.Camera
import com.example.scopeinspector.permissions.AppPermission.RecordAudio
import com.example.scopeinspector.permissions.PermissionState.Denied
import com.example.scopeinspector.permissions.PermissionState.Granted
import com.example.scopeinspector.permissions.PermissionState.NotApplicable
import com.example.scopeinspector.permissions.PermissionState.PermanentlyDenied
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRequestPlanTest {

    private val both = listOf(Camera, RecordAudio)
    private val never: (AppPermission) -> Boolean = { false }

    private fun plan(
        states: Map<AppPermission, PermissionState>,
        permissions: List<AppPermission> = both,
        rationale: (AppPermission) -> Boolean = never,
    ) = planPermissionRequest(permissions, states, rationale)

    @Test
    fun everythingGrantedMeansNothingToDo() {
        assertEquals(
            PermissionRequestPlan.NoAction,
            plan(mapOf(Camera to Granted, RecordAudio to Granted)),
        )
    }

    @Test
    fun permissionsThatDoNotApplyOnThisAndroidVersionAreNotRequested() {
        assertEquals(
            PermissionRequestPlan.NoAction,
            plan(mapOf(Camera to NotApplicable, RecordAudio to NotApplicable)),
        )
    }

    @Test
    fun grantedPlusNotApplicableMeansNothingToDo() {
        assertEquals(
            PermissionRequestPlan.NoAction,
            plan(mapOf(Camera to Granted, RecordAudio to NotApplicable)),
        )
    }

    @Test
    fun anEmptyPermissionListMeansNothingToDo() {
        assertEquals(PermissionRequestPlan.NoAction, plan(emptyMap(), permissions = emptyList()))
    }

    @Test
    fun aDeniedPermissionWithoutRationaleIsRequestedDirectly() {
        assertEquals(
            PermissionRequestPlan.Launch(listOf(Camera)),
            plan(mapOf(Camera to Denied, RecordAudio to Granted)),
        )
    }

    @Test
    fun onlyTheMissingPermissionsAreRequested() {
        assertEquals(
            PermissionRequestPlan.Launch(listOf(RecordAudio)),
            plan(mapOf(Camera to Granted, RecordAudio to Denied)),
        )
    }

    @Test
    fun aDeniedPermissionWithRationaleShowsTheExplanationFirst() {
        assertEquals(
            PermissionRequestPlan.ShowRationale(listOf(Camera)),
            plan(mapOf(Camera to Denied, RecordAudio to Granted), rationale = { it == Camera }),
        )
    }

    @Test
    fun theRationaleAppliesToAllRequestedPermissionsIfAnyNeedsIt() {
        assertEquals(
            PermissionRequestPlan.ShowRationale(listOf(Camera, RecordAudio)),
            plan(mapOf(Camera to Denied, RecordAudio to Denied), rationale = { it == RecordAudio }),
        )
    }

    @Test
    fun aRationaleForAPermissionThatIsNotRequestedIsIgnored() {
        // RecordAudio ya está concedido: aunque el sistema "sugiriera" explicarlo, no hay nada que pedir de él.
        assertEquals(
            PermissionRequestPlan.Launch(listOf(Camera)),
            plan(mapOf(Camera to Denied, RecordAudio to Granted), rationale = { it == RecordAudio }),
        )
    }

    @Test
    fun onlyPermanentlyDeniedPermissionsSendTheUserToSettings() {
        assertEquals(
            PermissionRequestPlan.OpenSettings,
            plan(mapOf(Camera to PermanentlyDenied, RecordAudio to Granted)),
        )
    }

    @Test
    fun permanentlyDeniedAndNotApplicableSendTheUserToSettings() {
        assertEquals(
            PermissionRequestPlan.OpenSettings,
            plan(mapOf(Camera to PermanentlyDenied, RecordAudio to NotApplicable)),
        )
    }

    @Test
    fun withAMixOfDeniedAndPermanentlyDeniedTheRequestablePartIsRequested() {
        // Se pide lo que se puede pedir; lo bloqueado se resolverá en la siguiente pulsación (Ajustes).
        assertEquals(
            PermissionRequestPlan.Launch(listOf(RecordAudio)),
            plan(mapOf(Camera to PermanentlyDenied, RecordAudio to Denied)),
        )
    }

    @Test
    fun theRequestedPermissionsKeepTheOrderOfTheModule() {
        val states = mapOf(RecordAudio to Denied, Camera to Denied)
        assertEquals(
            PermissionRequestPlan.Launch(listOf(Camera, RecordAudio)),
            plan(states, permissions = listOf(Camera, RecordAudio)),
        )
    }

    @Test
    fun aPermissionWithoutKnownStateIsNotRequested() {
        assertEquals(
            PermissionRequestPlan.NoAction,
            plan(mapOf(Camera to Granted)),
        )
    }
}
