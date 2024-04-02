package com.itangcent.idea.plugin.settings.helper

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.openapi.ui.Messages
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.idea.plugin.settings.EcsbExportMode
import com.itangcent.idea.plugin.settings.SettingBinder
import com.itangcent.idea.plugin.settings.update
import com.itangcent.idea.swing.MessagesHelper
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.logger.Logger
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Utility class providing access setting about ecsb through [SettingBinder]&[ConfigReader].
 */
@Singleton
class EcsbSettingsHelper {

    @Inject
    private lateinit var logger: Logger

    @Inject
    private lateinit var settingBinder: SettingBinder

    @Inject
    private lateinit var configReader: ConfigReader

    @Inject(optional = true)
    private val ecsbTokenChecker: EcsbTokenChecker? = null

    @Inject
    private lateinit var messagesHelper: MessagesHelper

    @Volatile
    private var server: String? = null

    @Volatile
    private var jwttoken: String? = null

    protected var cacheLock: ReadWriteLock = ReentrantReadWriteLock()

    //region server----------------------------------------------------

    fun hasServer(): Boolean {
        return getServer().notNullOrEmpty()
    }

    fun getServer(dumb: Boolean = true): String? {
        if (server.notNullOrBlank()) return server
        configReader.first("ecsb.server")?.trim()?.removeSuffix("/")
            ?.takeIf { it.notNullOrBlank() }
            ?.let {
                server = it
                return server
            }
        settingBinder.read().ecsbServer?.trim()?.removeSuffix("/")
            ?.takeIf { it.notNullOrBlank() }
            ?.let {
                server = it
                return server
            }
        if (!dumb) {
            val ecsbServer =
                messagesHelper.showInputDialog("Input server of ecsb",
                    "Server Of Ecsb", Messages.getInformationIcon())
                    ?.removeSuffix("/")
            if (ecsbServer.isNullOrBlank()) return null
            server = ecsbServer
            settingBinder.update {
                this.ecsbServer = ecsbServer
            }
            return ecsbServer
        }
        return null
    }

    //endregion server----------------------------------------------------

    //region jwttoken----------------------------------------------------

    fun getJwttoken(dumb: Boolean = true): String? {
        if (jwttoken.notNullOrBlank()) return jwttoken
        configReader.first("ecsb.jwttoken")?.trim()?.removeSuffix("/")
            ?.takeIf { it.notNullOrBlank() }
            ?.let {
                jwttoken = it
                return jwttoken
            }
        settingBinder.read().ecsbJwttoken?.trim()?.removeSuffix("/")
            ?.takeIf { it.notNullOrBlank() }
            ?.let {
                jwttoken = it
                return jwttoken
            }
        if (!dumb) {
            val ecsbJwttoken =
                messagesHelper.showInputDialog("Input jwttoken of ecsb",
                    "Jwttoken Of Ecsb", Messages.getInformationIcon())
                    ?.removeSuffix("/")
            if (ecsbJwttoken.isNullOrBlank()) return null
            jwttoken = ecsbJwttoken
            settingBinder.update {
                this.ecsbJwttoken = ecsbJwttoken
            }
            return ecsbJwttoken
        }
        return null
    }

    //endregion jwttoken----------------------------------------------------

    //region tokens----------------------------------------------

    /**
     * Tokens in setting.
     * Map<module,<token,state>>
     * state: null->unchecked,true->valid, false->invalid
     */
    private var tokenMap: HashMap<String, Pair<String, Boolean?>>? = null

    private var tryInputTokenOfModule: HashSet<String> = HashSet()

    fun getPrivateToken(module: String, dumb: Boolean = true): String? {

        configReader.first("ecsb.token.$module")?.let { return it }

        cacheLock.readLock().withLock {
            if (tokenMap != null) {
                tokenMap!![module]?.checked()?.let { return it }
            }
        }

        cacheLock.writeLock().withLock {
            if (tokenMap == null) {
                initToken()
            }
            tokenMap!![module]?.checked()?.let { return it }
            if (!dumb && tryInputTokenOfModule.add(module)) {
                val modulePrivateToken = inputNewToken(module)
                if (modulePrivateToken.notNullOrBlank()
                    && ecsbTokenChecker?.checkToken(modulePrivateToken!!) != false
                ) {
                    setToken(module, modulePrivateToken!!)
                    return modulePrivateToken
                }
            }
        }
        return null
    }

    private fun inputNewToken(module: String): String? {
        val inputTitle = if (loginMode()) "ProjectId" else "Private Token"
        return messagesHelper.showInputDialog("Input $inputTitle Of Module:$module",
            "Ecsb $inputTitle", Messages.getInformationIcon())
    }

    fun inputNewToken(): String? {
        val inputTitle = if (loginMode()) "ProjectId" else "Private Token"
        return messagesHelper.showInputDialog("Input $inputTitle",
            "Ecsb $inputTitle", Messages.getInformationIcon())
    }

    private fun Pair<String, Boolean?>.checked(): String? {
        return when (this.second) {
            null -> {
                val status = ecsbTokenChecker?.checkToken(this.first) ?: true
                updateTokenStatus(this.first, status)
                if (!status) {
                    logger.warn("token:${this.first} may be invalid.")
                    if (!settingBinder.read().loginMode && this.first.length != 64) {
                        logger.info("Please switch to loginModel if the version of ecsb is before 1.6.0")
                        logger.info("For more details see: http://easyyapi.com/documents/login_mode_yapi.html")
                    }
                }
                return if (status) this.first else null
            }
            true -> {
                this.first
            }
            false -> {
                null
            }
        }
    }

    /**
     * disable this token temporarily
     */
    fun disableTemp(token: String) {
        cacheLock.writeLock().withLock {
            if (tokenMap == null) {
                initToken()
            }
            updateTokenStatus(token, false)
        }
    }

    private fun updateTokenStatus(token: String, status: Boolean) {
        tokenMap!!.entries.forEach {
            if (it.value.first == token) {
                it.setValue(it.value.first to status)
            }
        }
    }

    private fun initToken() {
        tokenMap = HashMap()
        val settings = settingBinder.read()
        if (settings.ecsbTokens != null) {
            val properties = Properties()
            properties.load(settings.ecsbTokens!!.byteInputStream())
            properties.forEach { t, u -> tokenMap!![t.toString()] = u.toString() to null }
        }
    }

    private fun updateTokens(handle: (Properties) -> Unit) {
        cacheLock.writeLock().withLock {
            val settings = settingBinder.read()
            val properties = Properties()
            if (settings.ecsbTokens != null) {
                properties.load(settings.ecsbTokens!!.byteInputStream())
            }
            handle(properties)

            settings.ecsbTokens = ByteArrayOutputStream().also { properties.store(it, "") }.toString()
            settingBinder.save(settings)
            if (tokenMap == null) {
                tokenMap = HashMap()
            } else {
                tokenMap!!.clear()
            }
            properties.forEach { t, u -> tokenMap!![t.toString()] = u.toString() to null }
        }
    }

    fun setToken(module: String, token: String) {
        updateTokens { properties ->
            properties[module] = token
        }
        tokenMap?.put(module, token to null)
    }

    fun removeTokenByModule(module: String) {
        updateTokens { properties ->
            properties.remove(module)
        }
        tokenMap?.remove(module)
    }

    fun removeToken(token: String) {
        updateTokens { properties ->
            val removedKeys = properties.entries
                .filter { it.value == token }
                .map { it.key }
                .toList()
            removedKeys.forEach {
                properties.remove(it)
                tokenMap?.remove(it)
            }
        }
    }

    fun readTokens(): HashMap<String, String> {
        if (tokenMap == null) {
            initToken()
        }
        return HashMap(tokenMap!!.mapValues { it.value.first })
    }

    fun rawToken(token: String): String {
        if (loginMode()) {
            return ""
        }
        return token
    }

    //endregion  tokens----------------------------------------------

    fun enableUrlTemplating(): Boolean {
        return settingBinder.read().enableUrlTemplating
    }

    fun loginMode(): Boolean {
        return settingBinder.read().loginMode
    }

    fun exportMode(): EcsbExportMode {
        return EcsbExportMode.valueOf(settingBinder.read().ecsbExportMode)
    }

//    fun overwrite()

    fun switchNotice(): Boolean {
        return settingBinder.read().switchNotice
    }

    fun ecsbReqBodyJson5(): Boolean {
        return settingBinder.read().ecsbReqBodyJson5
    }

    fun ecsbResBodyJson5(): Boolean {
        return settingBinder.read().ecsbResBodyJson5
    }
}

/**
 * Performs checks on each {@code token} of ecsb.
 */
interface EcsbTokenChecker {

    /**
     * @return return true if the token is valid.
     */
    fun checkToken(token: String): Boolean
}