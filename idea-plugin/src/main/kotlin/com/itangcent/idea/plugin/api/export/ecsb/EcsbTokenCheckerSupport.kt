package com.itangcent.idea.plugin.api.export.ecsb

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.idea.plugin.settings.helper.EcsbTokenChecker

@Singleton
class EcsbTokenCheckerSupport : EcsbTokenChecker {

    @Inject
    private lateinit var ecsbApiHelper: EcsbApiHelper

    override fun checkToken(token: String): Boolean {
        return ecsbApiHelper.getProjectInfo(token) != null
    }
}