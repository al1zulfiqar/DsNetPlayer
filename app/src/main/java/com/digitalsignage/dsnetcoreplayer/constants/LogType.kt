package com.digitalsignage.dsnetcoreplayer.constants

enum class LogType(val value: Int) {
    PROOF_OF_PLAY(4),
    CONNECTIVITY_ERROR(1),
    DEVICE_ERROR(2),
    CONTENT_ERROR(3),
    NONE(-1);
}