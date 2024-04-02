package com.itangcent.idea.plugin.api.export.ecsb

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.itangcent.common.logger.Log
import com.itangcent.idea.plugin.api.export.core.DefaultLinkResolver
import com.itangcent.idea.utils.ModuleHelper
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.psi.PsiClassUtils
import org.apache.commons.lang3.StringUtils

class EcsbLinkResolver : DefaultLinkResolver() {

    @Inject
    protected val docHelper: DocHelper? = null

    override fun linkToMethod(linkMethod: Any): String? {
        if (linkMethod !is PsiMethod) {
            return "[$linkMethod]"
        }

        return super.linkToMethod(linkMethod)
    }

    companion object : Log()
}