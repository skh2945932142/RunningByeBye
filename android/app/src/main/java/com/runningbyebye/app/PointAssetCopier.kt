package com.runningbyebye.app

import java.io.File
import java.io.InputStream

object PointAssetCopier {
    private val requiredFields = arrayOf("T1001", "T1005", "T1014")

    fun copy(
        pointsDir: File,
        listAssets: (String) -> Array<String>?,
        openAsset: (String) -> InputStream,
    ) {
        pointsDir.mkdirs()

        for (field in requiredFields) {
            val assetPath = "points/$field"
            val files = listAssets(assetPath)
                ?.filter { it.endsWith(".json") }
                .orEmpty()

            if (files.isEmpty()) {
                throw IllegalStateException("APK 内缺少 $assetPath 点位资源")
            }

            val fieldDir = File(pointsDir, field)
            fieldDir.mkdirs()

            for (file in files) {
                openAsset("$assetPath/$file").use { input ->
                    File(fieldDir, file).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
