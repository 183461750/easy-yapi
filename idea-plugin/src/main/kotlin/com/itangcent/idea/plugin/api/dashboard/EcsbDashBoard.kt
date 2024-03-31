package com.itangcent.idea.plugin.api.dashboard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.idea.plugin.dialog.EcsbDashboardDialog
import com.itangcent.idea.plugin.dialog.YapiDashboardDialog
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.util.UIUtils

@Singleton
class EcsbDashBoard {

    @Inject
    private val actionContext: ActionContext? = null

    fun showDashBoardWindow() {
        val apiDashboardDialog = actionContext!!.instance { EcsbDashboardDialog() }
        UIUtils.show(apiDashboardDialog)
    }
}