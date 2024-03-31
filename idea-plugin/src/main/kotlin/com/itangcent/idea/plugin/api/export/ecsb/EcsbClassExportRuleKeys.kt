package com.itangcent.idea.plugin.api.export.ecsb

import com.itangcent.idea.plugin.rule.MyStringRuleMode
import com.itangcent.intellij.config.rule.*

object EcsbClassExportRuleKeys {

    val BEFORE_EXPORT: RuleKey<String> = SimpleRuleKey(
        "ecsb.export.before",
        EventRuleMode.THROW_IN_ERROR
    )

    val BEFORE_SAVE: RuleKey<String> = SimpleRuleKey(
        "ecsb.save.before",
        EventRuleMode.THROW_IN_ERROR
    )

    val AFTER_SAVE: RuleKey<String> = SimpleRuleKey(
        "ecsb.save.after",
        EventRuleMode.THROW_IN_ERROR
    )

    val TAG: RuleKey<String> = SimpleRuleKey(
        "api.tag",
        StringRuleMode.MERGE_DISTINCT
    )

    val STATUS: RuleKey<String> = SimpleRuleKey(
        "api.status", StringRuleMode.SINGLE
    )

    val FIELD_MOCK: RuleKey<String> = SimpleRuleKey(
        "field.mock",
        StringRuleMode.SINGLE
    )

    val FIELD_ADVANCED: RuleKey<List<String>> = SimpleRuleKey(
        "field.advanced",
        MyStringRuleMode.LIST
    )

    val PARAM_DEMO: RuleKey<String> = SimpleRuleKey(
        "param.demo",
        StringRuleMode.SINGLE
    )

    val OPEN: RuleKey<Boolean> = SimpleRuleKey(
        "api.open",
        BooleanRuleMode.ANY
    )

    val AFTER_FORMAT: RuleKey<String> = SimpleRuleKey(
        "ecsb.format.after",
        EventRuleMode.THROW_IN_ERROR
    )
}