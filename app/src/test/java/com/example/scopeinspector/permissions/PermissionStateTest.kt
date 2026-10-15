package com.example.scopeinspector.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStateTest {

    private fun resolve(
        applicable: Boolean = true,
        granted: Boolean = false,
        requestedBefore: Boolean = false,
        shouldShowRationale: Boolean = false,
    ) = resolvePermissionState(applicable, granted, requestedBefore, shouldShowRationale)

    @Test
    fun notApplicableWinsOverEverythingElse() {
        assertEquals(PermissionState.NotApplicable, resolve(applicable = false, granted = true))
    }

    @Test
    fun grantedPermissionIsGranted() {
        assertEquals(PermissionState.Granted, resolve(granted = true, requestedBefore = true))
    }

    @Test
    fun neverRequestedPermissionIsDeniedNotPermanent() {
        assertEquals(PermissionState.Denied, resolve())
    }

    @Test
    fun deniedOnceWithRationaleIsStillRequestable() {
        assertEquals(
            PermissionState.Denied,
            resolve(requestedBefore = true, shouldShowRationale = true),
        )
    }

    @Test
    fun requestedBeforeWithoutRationaleIsPermanentlyDenied() {
        assertEquals(
            PermissionState.PermanentlyDenied,
            resolve(requestedBefore = true, shouldShowRationale = false),
        )
    }

    @Test
    fun aggregateOfNothingOrOnlyNotApplicableIsNotApplicable() {
        assertEquals(PermissionState.NotApplicable, aggregatePermissionState(emptyList()))
        assertEquals(
            PermissionState.NotApplicable,
            aggregatePermissionState(listOf(PermissionState.NotApplicable)),
        )
    }

    @Test
    fun aggregateIgnoresNotApplicablePermissions() {
        assertEquals(
            PermissionState.Granted,
            aggregatePermissionState(listOf(PermissionState.Granted, PermissionState.NotApplicable)),
        )
    }

    @Test
    fun aggregateWithAnyPermanentDenialIsPermanentlyDenied() {
        assertEquals(
            PermissionState.PermanentlyDenied,
            aggregatePermissionState(
                listOf(PermissionState.Granted, PermissionState.Denied, PermissionState.PermanentlyDenied),
            ),
        )
    }

    @Test
    fun aggregateWithMissingPermissionIsDenied() {
        assertEquals(
            PermissionState.Denied,
            aggregatePermissionState(listOf(PermissionState.Granted, PermissionState.Denied)),
        )
    }

    @Test
    fun aggregateOfDeniedAndNotApplicableIsDenied() {
        assertEquals(
            PermissionState.Denied,
            aggregatePermissionState(listOf(PermissionState.Denied, PermissionState.NotApplicable)),
        )
    }

    @Test
    fun aggregateOfPermanentlyDeniedAndNotApplicableIsPermanentlyDenied() {
        assertEquals(
            PermissionState.PermanentlyDenied,
            aggregatePermissionState(listOf(PermissionState.PermanentlyDenied, PermissionState.NotApplicable)),
        )
    }

    @Test
    fun grantedWinsEvenIfItWasRequestedBeforeAndRationaleIsOff() {
        assertEquals(
            PermissionState.Granted,
            resolvePermissionState(
                applicable = true,
                granted = true,
                requestedBefore = true,
                shouldShowRationale = false,
            ),
        )
    }
}
