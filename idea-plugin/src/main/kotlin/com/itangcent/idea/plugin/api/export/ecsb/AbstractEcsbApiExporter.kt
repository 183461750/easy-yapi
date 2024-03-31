package com.itangcent.idea.plugin.api.export.ecsb

import com.google.inject.Inject
import com.intellij.openapi.project.Project
import com.itangcent.common.model.Doc
import com.itangcent.idea.plugin.api.export.core.ClassExporter
import com.itangcent.idea.plugin.api.export.core.Folder
import com.itangcent.idea.plugin.api.export.core.FormatFolderHelper
import com.itangcent.idea.plugin.rule.SuvRuleContext
import com.itangcent.idea.plugin.rule.setDoc
import com.itangcent.idea.plugin.settings.SettingBinder
import com.itangcent.idea.plugin.settings.helper.EcsbSettingsHelper
import com.itangcent.idea.psi.resource
import com.itangcent.idea.utils.ModuleHelper
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger


open class AbstractEcsbApiExporter {

    @Inject
    protected lateinit var logger: Logger

    @Inject
    protected lateinit var ecsbApiHelper: EcsbApiHelper

    @Inject
    protected lateinit var ecsbSettingsHelper: EcsbSettingsHelper

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject
    protected val classExporter: ClassExporter? = null

    @Inject
    protected val moduleHelper: ModuleHelper? = null

    @Inject
    protected val ecsbFormatter: EcsbFormatter? = null

    @Inject
    protected val project: Project? = null

    @Inject
    protected val settingBinder: SettingBinder? = null

    @Inject
    protected val formatFolderHelper: FormatFolderHelper? = null

    @Inject
    protected lateinit var ruleComputer: RuleComputer

    /**
     * Get the token of the special module.
     * see https://hellosean1025.github.io/yapi/documents/project.html#token
     * Used to request openapi.
     * see https://hellosean1025.github.io/yapi/openapi.html
     */
    protected open fun getTokenOfModule(module: String): String? {
        return ecsbSettingsHelper.getPrivateToken(module, false)
    }

    protected open fun getCartForResource(resource: Any): CartInfo? {

        //get token
        val module = actionContext.callInReadUI { moduleHelper!!.findModule(resource) } ?: return null
        val privateToken = getTokenOfModule(module)
        if (privateToken == null) {
            logger.info("No token be found for $module")
            return null
        }

        //get cart
        return getCartForResource(resource, privateToken)
    }

    protected fun getCartForResource(resource: Any, privateToken: String): CartInfo? {
        val folder = formatFolderHelper!!.resolveFolder(resource)
        return getCartForFolder(folder, privateToken)
    }

    protected open fun getCartForFolder(folder: Folder, privateToken: String): CartInfo? {
        return ecsbApiHelper.getCartForFolder(folder, privateToken)
    }

    fun exportDoc(doc: Doc): Boolean {
        if (doc.resource == null) return false
        val cartInfo = getCartForResource(doc.resource!!) ?: return false
        return exportDoc(doc, cartInfo.privateToken!!)
    }

    open fun exportDoc(doc: Doc, privateToken: String): Boolean {
        val items = ecsbFormatter!!.doc2Items(doc)
        var ret = false
        items.forEach { apiInfo ->
            apiInfo["token"] = privateToken
            apiInfo["switch_notice"] = ecsbSettingsHelper.switchNotice()

            val suvRuleContext = SuvRuleContext(doc.resource())
            suvRuleContext.setDoc(doc)
            suvRuleContext.setExt("ecsbInfo", apiInfo)

            ruleComputer.computer(
                EcsbClassExportRuleKeys.BEFORE_SAVE, suvRuleContext,
                doc.resource()
            )

            ret = ret or ecsbApiHelper.saveApiInfo(apiInfo)

            ruleComputer.computer(
                EcsbClassExportRuleKeys.AFTER_SAVE, suvRuleContext,
                doc.resource()
            )
        }
        return ret
    }
}