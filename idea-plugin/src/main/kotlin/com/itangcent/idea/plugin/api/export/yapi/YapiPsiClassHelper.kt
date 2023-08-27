package com.itangcent.idea.plugin.api.export.yapi

import com.intellij.psi.PsiElement
import com.itangcent.common.constant.Attrs
import com.itangcent.common.kit.KVUtils
import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.common.utils.sub
import com.itangcent.idea.plugin.api.export.AdditionalField
import com.itangcent.idea.utils.CustomizedPsiClassHelper
import com.itangcent.intellij.config.rule.computer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.psi.ContextSwitchListener
import com.itangcent.intellij.psi.ResolveContext

/**
 * support rules:
 * 1. field.mock
 * 2. field.demo
 * 3. field.advanced
 */
class YapiPsiClassHelper : CustomizedPsiClassHelper() {

    private var resolveProperty: Boolean = true

    @PostConstruct
    fun initYapiInfo() {
        val contextSwitchListener: ContextSwitchListener? = ActionContext.getContext()
            ?.instance(ContextSwitchListener::class)
        contextSwitchListener!!.onModuleChange {
            val resolveProperty = configReader.first("field.mock.resolveProperty")
            if (!resolveProperty.isNullOrBlank()) {
                this.resolveProperty = resolveProperty.toBoolean()
            }
        }
    }

    override fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>,
    ) {
        //compute `field.mock`
        ruleComputer.computer(YapiClassExportRuleKeys.FIELD_MOCK, fieldOrMethod)
            ?.takeIf { it.isNotBlank() }
            ?.let { if (resolveProperty) configReader.resolveProperty(it) else it }
            ?.let { mockInfo ->
                fields.sub(Attrs.MOCK_ATTR)[fieldName] = mockInfo
                parseAsFieldValue(mockInfo)
                    ?.also { KVUtils.useFieldAsAttr(it, Attrs.MOCK_ATTR) }
                    ?.let { populateFieldValue(fieldName, fieldType, fields, it) }
            }

        //compute `field.advanced`
        val advancedValue = ruleComputer.computer(
            YapiClassExportRuleKeys.FIELD_ADVANCED,
            fieldOrMethod
        )
        if (advancedValue.notNullOrEmpty()) {
            fields.sub(Attrs.ADVANCED_ATTR)[fieldName] = advancedValue
        }

        super.afterParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, resolveContext, fields)
    }

    override fun resolveAdditionalField(
        additionalField: AdditionalField,
        context: PsiElement,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>,
    ) {
        super.resolveAdditionalField(additionalField, context, resolveContext, fields)
        val fieldName = additionalField.name!!
        additionalField.getExt<Any>(Attrs.MOCK_ATTR)?.let {
            fields.sub(Attrs.MOCK_ATTR)[fieldName] = it
        }
        additionalField.getExt<Any>(Attrs.DEMO_ATTR)?.let {
            fields.sub(Attrs.DEMO_ATTR)[fieldName] = it
        }
        additionalField.getExt<Any>(Attrs.ADVANCED_ATTR)?.let {
            fields.sub(Attrs.ADVANCED_ATTR)[fieldName] = it
        }
    }
}