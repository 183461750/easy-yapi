package com.itangcent.idea.plugin.api.export.ecsb

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.common.kit.toJson
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

            val paramValue = getBody(apiInfo)
            val ecsbReturnValue = httpClientProvide!!.getHttpClient()
                .post(ecsbSettingsHelper.getServer(false) + ECSB_SAVE_API)
                .contentType(ContentType.APPLICATION_FORM_URLENCODED.withCharset(Consts.UTF_8))
                .header("Cookie", "jwttoken=" + ecsbSettingsHelper.getJwttoken())
                .header("tanentId", "00")
                .param("data", paramValue)
                .call()
                .use { it.string() }
                ?.trim()

            val errorMsg = findErrorMsg(ecsbReturnValue)
            if (StringUtils.isNotBlank(errorMsg)) {
                logger.info("save apiInfo failed:$errorMsg")
                logger.info("paramValue:${GsonUtils.toJson(paramValue)}")
                return false
            }

//            GsonUtils.parseToJsonTree(ecsbReturnValue)
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

        // TODO @Fa 需要搞成读取配置文件
        var addTxtAbbreviationPrefix = "";
// addTxtAbbreviationPrefix = "marketing";

        // url路径
        var path = apiInfo?.get("path") as String
        // url名称
        var apiDesc = apiInfo?.get("title") as String
        // TODO @Fa 需要搞成读取配置文件
        var host = "http://sitapi.jxs.crb.cn"

        // TODO @Fa 需要搞成读取配置文件
        var hostPrefix = "/crb-marketing-api-sec";

        logger.info("save apiInfo ecsbReqValue: path:$path apiDesc:$apiDesc")

        var pathHump = path
        var secondSlashIndex = pathHump.indexOf('/', 1)
        do {
            pathHump = pathHump.substring(0, secondSlashIndex) + pathHump.substring(secondSlashIndex + 1).replaceFirstChar { it.uppercase() }
            secondSlashIndex = pathHump.indexOf('/', 1)
        } while (secondSlashIndex > 0);
        pathHump = pathHump.substring(1)

        pathHump = if (addTxtAbbreviationPrefix.isNotBlank()) {
            addTxtAbbreviationPrefix + pathHump.replaceFirstChar { it.uppercase() }
        } else {
            pathHump
        }

        var jsonBodyStr = """
            {    "baseInfo": {        "sysId": "12000008",        "appId": "1200000801XS",        "sysPrimaryId": 17,        "appPrimaryId": 25,        "apiId": "snowbeer.ocms.OCMS.{{addTxtAbbreviation}}",        "addTxtAbbreviation": "{{addTxtAbbreviation}}",        "columnCode": "public",        "columnLabel": "",        "apiDesc": "{{apiDesc}}",        "serverType": 0,        "statusCodePolicy": 0,        "templateId": "",        "reqXls": "",        "respXls": "",        "usableGateway": [            1,            2,            6        ],        "isOauth": 1,        "oauthSource": "",        "method": "POST",        "isMsgCache": 0,        "noRepeatCommit": 0,        "timeout": 10,        "msgCacheTime": "",        "isMock": 0,        "mockConfigId": "",        "describe": "",        "apiPrifix": "snowbeer.ocms.OCMS.",        "orgPrimaryId": "",        "orgId": "12000000",        "isBasicAuth": 0,        "reqRootname": "",        "respRootname": ""    },    "basicInfo": {        "username": "",        "password": ""    },    "versionInfos": [        {            "versionId": "1.0",            "apiId": "",            "status": 2,            "faPath": "/",            "loadBalancingStrategy": 1,            "limitNumber": 0,            "isFusing": 0,            "totalDegree": 0,            "timeQuantum": 0,            "failureRate": 0,            "errorCode": "",            "successExample": "",            "errorExample": "",            "mockConfigId": "",            "isParamRule": 0,            "paramRuleId": ""        }    ],    "paramInfos": [],    "urlInfos": [        {            "baPath": "",            "baServiceAddress": "{{url}}",            "url": "{{url}}",            "versionId": "1.0",            "weight": 1,            "zoneType": 0        }    ],    "errorCodeInfos": [],    "sysInfos": [        {            "type": 1,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "appInfos": [        {            "type": 2,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "insInfos": [        {            "type": 6,            "versionId": "1.0",            "signType": 1,            "transMode": "2",            "isMsgSecret": 0,            "isCheckTimestamp": 1        }    ],    "yunInfos": []}
        """.trimIndent()

        return jsonBodyStr.replace("{{addTxtAbbreviation}}", pathHump + sit2)
            .replace("{{apiDesc}}", apiDesc)
            .replace("{{url}}", host + hostPrefix + path)
    }

    companion object : Log() {
        const val GET_CAT = "/api/interface/list_cat"

        const val ECSB_SAVE_API = "/apis/ecsb/apiBaseInfo/saveApiInterface/0"

    }
}