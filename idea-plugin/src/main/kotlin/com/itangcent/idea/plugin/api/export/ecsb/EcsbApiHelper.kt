package com.itangcent.idea.plugin.api.export.ecsb

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.inject.ImplementedBy
import com.itangcent.common.logger.traceError
import com.itangcent.idea.plugin.api.export.core.Folder
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.sub
import com.itangcent.intellij.logger.Logger


@ImplementedBy(DefaultEcsbApiHelper::class)
interface EcsbApiHelper {

    //apis--------------------------------------------------------------

    fun findApis(token: String, catId: String): List<Any?>?

    fun listApis(token: String, catId: String, limit: Int?): JsonArray?

    fun saveApiInfo(apiInfo: HashMap<String, Any?>): Boolean

    //projects--------------------------------------------------------------

    fun getProjectIdByToken(token: String): String?

    fun getProjectInfo(token: String, projectId: String?): JsonObject?

    fun getProjectInfo(token: String): JsonObject?

}

fun EcsbApiHelper.listApis(token: String, catId: String): JsonArray? {
    return this.listApis(token, catId, null)
}
