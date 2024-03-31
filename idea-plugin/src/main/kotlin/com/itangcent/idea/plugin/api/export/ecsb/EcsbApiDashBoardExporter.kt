package com.itangcent.idea.plugin.api.export.ecsb

import com.intellij.util.containers.ContainerUtil
import com.itangcent.common.model.Doc
import com.itangcent.idea.plugin.api.export.core.Folder
import java.util.*
import kotlin.collections.set


class EcsbApiDashBoardExporter : AbstractEcsbApiExporter() {

    private var successExportedCarts: MutableSet<String> = ContainerUtil.newConcurrentSet<String>()

    //privateToken+folderName -> CartInfo
    private val folderNameCartMap: HashMap<String, CartInfo> = HashMap()

    @Synchronized
    override fun getCartForFolder(folder: Folder, privateToken: String): CartInfo? {
        var cartInfo = folderNameCartMap["$privateToken${folder.name}"]
        if (cartInfo != null) return cartInfo

        cartInfo = super.getCartForFolder(folder, privateToken)
        if (cartInfo != null) {
            folderNameCartMap["$privateToken${folder.name}"] = cartInfo
        }
        return cartInfo
    }

    override fun exportDoc(doc: Doc, privateToken: String): Boolean {
        if (doc.resource == null) return false
        return super.exportDoc(doc, privateToken)
    }

}