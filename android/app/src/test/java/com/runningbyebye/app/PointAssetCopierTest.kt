package com.runningbyebye.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class PointAssetCopierTest {
    @Test
    fun existingPointsDirectoryDoesNotSkipMissingFields() {
        val root = Files.createTempDirectory("point-assets").toFile()
        val pointsDir = root.resolve("points")
        pointsDir.resolve("T1001").mkdirs()
        pointsDir.resolve("T1001/old.json").writeText("{}")

        val assets = mapOf(
            "points/T1001" to arrayOf("fenghua.json"),
            "points/T1005" to arrayOf("taiji.json"),
            "points/T1014" to arrayOf("ningjing.json"),
        )

        PointAssetCopier.copy(
            pointsDir = pointsDir,
            listAssets = { path -> assets[path] },
            openAsset = { ByteArrayInputStream("""{"data":[]}""".toByteArray()) },
        )

        assertTrue(pointsDir.resolve("T1001/fenghua.json").isFile)
        assertTrue(pointsDir.resolve("T1005/taiji.json").isFile)
        assertTrue(pointsDir.resolve("T1014/ningjing.json").isFile)
    }

    @Test(expected = IllegalStateException::class)
    fun missingApkAssetFieldFailsClearly() {
        val pointsDir = Files.createTempDirectory("point-assets").toFile().resolve("points")
        val assets = mapOf(
            "points/T1001" to arrayOf("fenghua.json"),
            "points/T1005" to emptyArray<String>(),
            "points/T1014" to arrayOf("ningjing.json"),
        )

        PointAssetCopier.copy(
            pointsDir = pointsDir,
            listAssets = { path -> assets[path] },
            openAsset = { ByteArrayInputStream("""{"data":[]}""".toByteArray()) },
        )
    }
}
