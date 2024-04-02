package com.itangcent.idea.plugin.api.export.ecsb

import com.google.inject.Inject
import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.ContainerUtil
import com.itangcent.common.logger.traceError
import com.itangcent.common.model.Doc
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.idea.plugin.api.ClassApiExporterHelper
import com.itangcent.idea.plugin.api.export.core.Folder
import com.itangcent.idea.swing.MessagesHelper
import com.itangcent.intellij.extend.withBoundary
import com.itangcent.intellij.psi.SelectedHelper
import com.itangcent.intellij.util.ActionUtils
import com.itangcent.intellij.util.FileType
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


class EcsbApiExporter : AbstractEcsbApiExporter() {

    @Inject
    private lateinit var classApiExporterHelper: ClassApiExporterHelper

    fun export() {
        val serverFound = ecsbSettingsHelper.getServer(false).notNullOrBlank()
        if (serverFound) {
            doExport()
        }
    }

    private fun doExport() {
        var anyFound = false
        classApiExporterHelper.export {
            anyFound = true
            exportDoc(it)
        }
        if (anyFound) {
            logger.info("Apis exported completed")
        } else {
            logger.info("No api be found to export!")
        }
    }

}