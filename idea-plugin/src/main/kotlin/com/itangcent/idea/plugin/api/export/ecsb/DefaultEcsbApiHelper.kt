package com.itangcent.idea.plugin.api.export.ecsb

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.asInt
import com.itangcent.common.utils.asMap
import com.itangcent.http.contentType
import com.itangcent.idea.plugin.utils.LocalStorage
import com.itangcent.idea.swing.MessagesHelper
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.asJsonElement
import com.itangcent.intellij.extend.asList
import com.itangcent.intellij.extend.asMutableList
import com.itangcent.intellij.extend.sub
import com.itangcent.spi.SpiCompositeLoader
import org.apache.commons.lang3.StringUtils
import org.apache.http.Consts
import org.apache.http.entity.ContentType
import kotlin.collections.set
import kotlin.concurrent.withLock

@Singleton
open class DefaultEcsbApiHelper : AbstractEcsbApiHelper(), EcsbApiHelper {

    //$projectId$cartName -> $cartId
    private var cartIdCache: HashMap<String, String> = HashMap()

    @Inject
    protected lateinit var localStorage: LocalStorage

    @Inject
    internal lateinit var messagesHelper: MessagesHelper

    @Inject
    internal lateinit var actionContext: ActionContext

    override fun getApiInfo(token: String, id: String): JsonObject? {
        val url = "${ecsbSettingsHelper.getServer()}$GET_INTERFACE?token=$token&id=$id"
        return GsonUtils.parseToJsonTree(getByApi(url))
            ?.sub("data")?.asJsonObject
    }

    override fun findApi(token: String, catId: String, apiName: String): String? {
        return listApis(token, catId)
            ?.firstOrNull { api ->
                api.sub("title")
                    ?.asString == apiName
            }?.sub("_id")?.asString
    }

    override fun findApis(token: String, catId: String): List<Any?>? {
        return listApis(token, catId)
            ?.asMutableList()
    }

    override fun listApis(token: String, catId: String, limit: Int?): JsonArray? {
        var apiLimit = limit ?: localStorage.get("__internal__", "ecsb.api.limit").asInt() ?: 1000
        val url = "${ecsbSettingsHelper.getServer()}$GET_CAT?token=$token&catid=$catId&limit=$apiLimit"
        val jsonArray = GsonUtils.parseToJsonTree(getByApi(url))
            ?.sub("data")
            ?.sub("list")
            ?.asJsonArray
        if (jsonArray?.size() == apiLimit && apiLimit < 5000) {
            apiLimit = (apiLimit * 1.4).toInt()
            localStorage.set("__internal__", "ecsb.api.limit", apiLimit)
            return listApis(token, catId, apiLimit)
        }
        return jsonArray
    }

    private val saveInterceptor: EcsbSaveInterceptor by lazy {
        SpiCompositeLoader.loadComposite()
    }

    override fun saveApiInfo(apiInfo: HashMap<String, Any?>): Boolean {
        if (!saveInterceptor.beforeSaveApi(this, apiInfo)) {
            return false
        }

        if (ecsbSettingsHelper.loginMode() && apiInfo.containsKey("token")) {
            apiInfo["project_id"] = apiInfo["token"]
            apiInfo.remove("token")
        }

        try {



            val ecsbReturnValue = httpClientProvide!!.getHttpClient()
                .post(ecsbSettingsHelper.getServer(false) + ECSB_SAVE_API)
                .contentType(ContentType.APPLICATION_FORM_URLENCODED.withCharset(Consts.UTF_8))
                .header("Cookie", "jwttoken=BearereyJhbGciOiJIUzUxMiJ9.eyJsb2dpblR5cGUiOiJsb2NhbCIsImxvZ2luTmFtZSI6Im9jbXNhZG1pbjEiLCJ1c2VyVGVuYW50SWQiOm51bGwsImluc3RhbmNlVHlwZSI6InJlbW90ZSIsImlkIjoxNywidXNlclR5cGUiOiIyIiwidXNlck5hbWUiOiLnu4_plIDllYborqLljZXnrqHnkIblkZgxIiwiZXhwIjoxNzE1NTAyNzcxLCJ1c2VySWQiOiIxNWZiMGUwMDc1MjU0YTIyOWIzNzc1ZGFhM2ViOWI2MiIsIm9yZ0lkIjoiMTIwMDAwMDAiLCJ0ZW5hbnRJZCI6bnVsbCwidGFuZW50SWQiOiIwMCIsImlzQWRtaW4iOiJmYWxzZSJ9.A9obmUGxOycxDx3GyU-P33lAxtSovngjz7qQ47LXJpJQy1wfctMm4tIPiUMBNTDp1AXWo7ZypfzOSSkfMiixuA")
                .header("tanentId", "00")
                .param("data", getBody(apiInfo))
                .call()
                .use { it.string() }
                ?.trim()
            logger.info("save apiInfo ecsbReturnValue:$ecsbReturnValue")



//            val returnValue = httpClientProvide!!.getHttpClient()
//                .post(ecsbSettingsHelper.getServer(false) + SAVE_API)
//                .contentType(ContentType.APPLICATION_JSON)
//                .body(apiInfo)
//                .call()
//                .use { it.string() }
//                ?.trim()
//            val errMsg = findErrorMsg(returnValue)
//            if (StringUtils.isNotBlank(errMsg)) {
//                logger.info("save apiInfo failed:$errMsg")
//                logger.info("api info:${GsonUtils.toJson(apiInfo)}")
//                return false
//            }
//            GsonUtils.parseToJsonTree(returnValue)
//                .sub("data")
//                ?.asList()
//                ?.firstOrNull()
//                ?.asMap()
//                ?.get("_id")
//                ?.toString()?.let {
//                    apiInfo["_id"] = it
//                    LOG.debug("save api: $it")
//                }
            return true
        } catch (e: Throwable) {
            logger.traceError("save apiInfo failed", e)
            return false
        }
    }

    fun getBody(apiInfo: HashMap<String, Any?>): String {

// snowbeer.ocms.OCMS.processTerminateTasksit2

        var sit2 = "";
        sit2 = "sit2";

        var addTxtAbbreviationPrefix = "";
// addTxtAbbreviationPrefix = "marketing";

//        var path = "/process/bpmCallback";
        var path = apiInfo?.get("path") as String
//        var apiDesc = "工作流回调接口";
        var apiDesc = apiInfo?.get("title") as String
        var host = "http://sitapi.jxs.crb.cn"

// hostPrefix = "/crb-mall-api-sec";
        var hostPrefix = "/crb-marketing-api-sec";

        logger.info("save apiInfo ecsbReqValue: path:$path apiDesc:$apiDesc")


        var secondSlashIndex = path.indexOf('/', 1);
//        var pathHump = path.substring(1, secondSlashIndex) + path.substring(secondSlashIndex + 1).replace(/[a-z]/, char => char.toUpperCase());
//
//        pathHump = addTxtAbbreviationPrefix ? addTxtAbbreviationPrefix + pathHump.replace(/[a-z]/, char => char.toUpperCase()) : pathHump;

        var pathHump = path.substring(1, secondSlashIndex) + path.substring(secondSlashIndex + 1).replaceFirstChar { it.uppercase() }
        pathHump = if (addTxtAbbreviationPrefix.isNotBlank()) {
            addTxtAbbreviationPrefix + pathHump.replaceFirstChar { it.uppercase() }
        } else {
            pathHump
        }

//        pm.environment.set("addTxtAbbreviation", pathHump + sit2);
//        pm.environment.set("apiDesc", apiDesc);
// http://sitapi.jxs.crb.cn/crb-mall-api-sec/popupPlan/saveOrUpdate
// http://uatocmsapi.crb.cn/crb-dealer-api-sec/dealer/getDealerByCode
//        pm.environment.set("url", host + hostPrefix + path);

//        console.log(pathHump)

        var jsonBodyStr = """
            {    "baseInfo": {        "sysId": "12000008",        "appId": "1200000801XS",        "sysPrimaryId": 17,        "appPrimaryId": 25,        "apiId": "snowbeer.ocms.OCMS.{{addTxtAbbreviation}}",        "addTxtAbbreviation": "{{addTxtAbbreviation}}",        "columnCode": "public",        "columnLabel": "",        "apiDesc": "{{apiDesc}}",        "serverType": 0,        "statusCodePolicy": 0,        "templateId": "",        "reqXls": "",        "respXls": "",        "usableGateway": [            1,            2,            6        ],        "isOauth": 1,        "oauthSource": "",        "method": "POST",        "isMsgCache": 0,        "noRepeatCommit": 0,        "timeout": 10,        "msgCacheTime": "",        "isMock": 0,        "mockConfigId": "",        "describe": "",        "apiPrifix": "snowbeer.ocms.OCMS.",        "orgPrimaryId": "",        "orgId": "12000000",        "isBasicAuth": 0,        "reqRootname": "",        "respRootname": ""    },    "basicInfo": {        "username": "",        "password": ""    },    "versionInfos": [        {            "versionId": "1.0",            "apiId": "",            "status": 2,            "faPath": "/",            "loadBalancingStrategy": 1,            "limitNumber": 0,            "isFusing": 0,            "totalDegree": 0,            "timeQuantum": 0,            "failureRate": 0,            "errorCode": "",            "successExample": "",            "errorExample": "",            "mockConfigId": "",            "isParamRule": 0,            "paramRuleId": ""        }    ],    "paramInfos": [],    "urlInfos": [        {            "baPath": "",            "baServiceAddress": "{{url}}",            "url": "{{url}}",            "versionId": "1.0",            "weight": 1,            "zoneType": 0        }    ],    "errorCodeInfos": [],    "sysInfos": [        {            "type": 1,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "appInfos": [        {            "type": 2,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "insInfos": [        {            "type": 6,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "yunInfos": []}
        """.trimIndent()

        return jsonBodyStr.replace("{{addTxtAbbreviation}}", pathHump + sit2)
            .replace("{{apiDesc}}", apiDesc)
            .replace("{{url}}", host + hostPrefix + path)
    }

    override fun getApiWeb(module: String, cartName: String, apiName: String): String? {
        val token = ecsbSettingsHelper.getPrivateToken(module)
        val projectId = getProjectIdByToken(token!!) ?: return null
        val catId = findCart(token, cartName) ?: return null
        val apiId = findApi(token, catId, apiName) ?: return null
        return "${ecsbSettingsHelper.getServer()}/project/$projectId/interface/api/$apiId"
    }

    override fun findCartWeb(module: String, cartName: String): String? {
        val token = ecsbSettingsHelper.getPrivateToken(module)
        val projectId = getProjectIdByToken(token!!) ?: return null
        val catId = findCart(token, cartName) ?: return null
        return getCartWeb(projectId, catId)
    }

    override fun getCartWeb(projectId: String, catId: String): String? {
        return "${ecsbSettingsHelper.getServer()}/project/$projectId/interface/api/cat_$catId"
    }

    override fun findCart(token: String, name: String): String? {
        val projectId: String = getProjectIdByToken(token) ?: return null
        val key = "$projectId$name"
        var cachedCartId = cacheLock.readLock().withLock { cartIdCache[key] }
        if (cachedCartId != null) return cachedCartId
        var projectInfo: JsonElement? = null
        try {
            projectInfo = getProjectInfo(token, projectId)
            val cats = projectInfo
                ?.sub("data")
                ?.sub("cat")
                ?.asJsonArray
            cats?.forEach { cat ->
                if (cat.sub("name")?.asString == name) {
                    cachedCartId = cat.sub("_id")!!
                        .asString
                    if (cachedCartId != null) {
                        cacheLock.writeLock().withLock {
                            cartIdCache[key] = cachedCartId!!
                        }
                    }
                    return cachedCartId
                }
            }
        } catch (e: Exception) {
            logger.traceError("error to find cat. projectId:$projectId, info: ${projectInfo?.toString()}", e)
        }
        return null
    }

    override fun addCart(privateToken: String, name: String, desc: String): Boolean {
        val projectId = getProjectIdByToken(privateToken) ?: return false
        return addCart(projectId, privateToken, name, desc)
    }

    override fun addCart(projectId: String, token: String, name: String, desc: String): Boolean {
        try {
            val returnValue = httpClientProvide!!.getHttpClient()
                .post(ecsbSettingsHelper.getServer(false) + ADD_CART)
                .contentType(ContentType.APPLICATION_JSON)
                .body(
                    linkedMapOf(
                        "desc" to desc,
                        "project_id" to projectId,
                        "name" to name,
                        "token" to ecsbSettingsHelper.rawToken(token)
                    )
                )
                .call()
                .use { it.string() }
                ?.trim()

            val errMsg = findErrorMsg(returnValue)
            if (StringUtils.isNotBlank(errMsg)) {
                logger.info("Post failed:$errMsg")
                return false
            }
            val resObj = returnValue?.asJsonElement()
            val addCartId: String? = resObj.sub("data")
                .sub("_id")
                ?.asString
            if (addCartId != null) {
                cacheLock.writeLock().withLock {
                    cartIdCache["$projectId$name"] = addCartId
                }
                logger.info("Add new cart:${ecsbSettingsHelper.getServer()}/project/$projectId/interface/api/cat_$addCartId")
            } else {
                logger.info("Add cart failed,response is:$returnValue")
            }
            cacheLock.writeLock().withLock { projectInfoCache.remove(projectId) }
            return true
        } catch (e: Throwable) {
            logger.traceError("Post failed", e)
            return false
        }
    }

    override fun findCarts(projectId: String, token: String): List<Any?>? {
        val url = "${ecsbSettingsHelper.getServer()}$GET_CAT_MENU?project_id=$projectId&token=$token"
        return GsonUtils.parseToJsonTree(getByApi(url))
            ?.sub("data")
            ?.asList()
    }

    companion object : Log() {
        const val GET_INTERFACE = "/api/interface/get"
        const val ADD_CART = "/api/interface/add_cat"
        const val GET_CAT_MENU = "/api/interface/getCatMenu"
        const val SAVE_API = "/api/interface/save"
        const val GET_CAT = "/api/interface/list_cat"

        const val ECSB_SAVE_API = "/apis/ecsb/apiBaseInfo/saveApiInterface/0"

    }
}