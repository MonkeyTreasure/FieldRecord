package com.fieldrecord.presentation.camera

internal fun isVideoTheLatestMedia(videoModifiedAt: Long?, photoModifiedAt: Long?): Boolean =
    videoModifiedAt != null && (photoModifiedAt == null || videoModifiedAt >= photoModifiedAt)
