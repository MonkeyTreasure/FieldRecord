package com.fieldrecord.presentation.map

internal enum class TemporaryGuidePinTapAction {
    SELECT,
    REQUEST_DELETE
}

internal fun temporaryGuidePinTapAction(selectedPinId: Long?, tappedPinId: Long): TemporaryGuidePinTapAction =
    if (selectedPinId == tappedPinId) TemporaryGuidePinTapAction.REQUEST_DELETE
    else TemporaryGuidePinTapAction.SELECT

internal data class TemporaryGuidePin(
    val id: Long,
    val latitude: Double,
    val longitude: Double
)