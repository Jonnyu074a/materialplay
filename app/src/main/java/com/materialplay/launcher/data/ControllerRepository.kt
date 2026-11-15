package com.materialplay.launcher.data

import com.materialplay.launcher.data.db.ControllerDao
import com.materialplay.launcher.data.db.ControllerEntity
import com.materialplay.launcher.gamepad.ConnectedController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ControllerRepository(private val dao: ControllerDao) {
    val savedControllers: Flow<List<ControllerEntity>> = dao.observeAll()

    suspend fun remember(controller: ConnectedController) = withContext(Dispatchers.IO) {
        val previous = dao.get(controller.identity)
        dao.upsert(
            ControllerEntity(
                descriptor = controller.identity,
                name = controller.displayName,
                vendorId = controller.vendorId,
                productId = controller.productId,
                connectionType = controller.connection.name,
                glyphProfile = previous?.glyphProfile ?: "auto",
                faceSwap = previous?.faceSwap,
                lastSeen = System.currentTimeMillis()
            )
        )
    }

    suspend fun setGlyphProfile(descriptor: String, profile: String) = withContext(Dispatchers.IO) {
        dao.setGlyphProfile(descriptor, profile)
    }

    suspend fun setFaceSwap(descriptor: String, swap: Boolean?) = withContext(Dispatchers.IO) {
        dao.setFaceSwap(descriptor, swap)
    }
}
