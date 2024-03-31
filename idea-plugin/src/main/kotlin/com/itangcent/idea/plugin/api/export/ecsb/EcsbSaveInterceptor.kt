package com.itangcent.idea.plugin.api.export.ecsb

import com.intellij.openapi.ui.Messages
import com.itangcent.common.concurrent.ValueHolder
import com.itangcent.idea.plugin.condition.ConditionOnSetting
import com.itangcent.idea.swing.MessagesHelper
import com.itangcent.intellij.context.ActionContext

/**
 * Workflow interface that allows for customized ecsb save action.
 */
internal interface EcsbSaveInterceptor {
    /**
     * Called before [EcsbApiHelper] save an apiInfo to ecsb server.
     *
     * @return @return {@code true} if the apiInfo should be saved.
     * Else, [EcsbApiHelper] will discard this apiInfo.
     */
    fun beforeSaveApi(apiHelper: EcsbApiHelper, apiInfo: HashMap<String, Any?>): Boolean
}

/**
 * Immutable [EcsbSaveInterceptor] that always return true.
 * This indicates that the apis will to be <b>always</b>
 * updated regardless whether the api is already existed
 * in ecsb server or not.
 *
 */
@ConditionOnSetting("ecsbExportMode", havingValue = "ALWAYS_UPDATE")
class AlwaysUpdateEcsbSaveInterceptor : EcsbSaveInterceptor {
    override fun beforeSaveApi(apiHelper: EcsbApiHelper, apiInfo: HashMap<String, Any?>): Boolean {
        return true
    }
}

val ALWAYS_UPDATE_API_SAVE_INTERCEPTOR = AlwaysUpdateEcsbSaveInterceptor()

/**
 * Immutable [EcsbSaveInterceptor] that never update existed api.
 * In that case nothing will change on the api which is already existed in ecsb server.
 */
@ConditionOnSetting("ecsbExportMode", havingValue = "NEVER_UPDATE")
class NeverUpdateEcsbSaveInterceptor : EcsbSaveInterceptor {
    override fun beforeSaveApi(apiHelper: EcsbApiHelper, apiInfo: HashMap<String, Any?>): Boolean {
        return !apiHelper.existed(apiInfo)
    }
}

val NEVER_UPDATE_API_SAVE_INTERCEPTOR = NeverUpdateEcsbSaveInterceptor()

/**
 * Immutable [EcsbSaveInterceptor] that is always ask whether to update or skip an existing API.
 */
@ConditionOnSetting("ecsbExportMode", havingValue = "ALWAYS_ASK")
class AlwaysAskEcsbSaveInterceptor : EcsbSaveInterceptor {

    private var selectedEcsbSaveInterceptor: EcsbSaveInterceptor? = null

    @Synchronized
    override fun beforeSaveApi(apiHelper: EcsbApiHelper, apiInfo: HashMap<String, Any?>): Boolean {
        if (selectedEcsbSaveInterceptor != null) {
            return selectedEcsbSaveInterceptor!!.beforeSaveApi(apiHelper, apiInfo)
        }
        if (!apiHelper.existed(apiInfo)) {
            return true
        }
        val valueHolder = ValueHolder<Boolean>()
        val context = ActionContext.getContext() ?: return true
        context.instance(MessagesHelper::class).showAskWithApplyAllDialog(
            "The api [${apiInfo["title"]}] already existed in the project.\n" +
                    "Do you want update it?", arrayOf("Update", "Skip", "Cancel")) { ret, applyAll ->
            if (ret == Messages.CANCEL) {
                context.stop()
                valueHolder.success(false)
                return@showAskWithApplyAllDialog
            }
            if (applyAll) {
                if (ret == Messages.YES) {
                    selectedEcsbSaveInterceptor = ALWAYS_UPDATE_API_SAVE_INTERCEPTOR
                } else if (ret == Messages.NO) {
                    selectedEcsbSaveInterceptor = NEVER_UPDATE_API_SAVE_INTERCEPTOR
                }
            }

            if (ret == Messages.YES) {
                valueHolder.success(true)
            } else {
                valueHolder.success(false)
            }
        }
        return valueHolder.value() ?: false
    }
}
