package com.itangcent.idea.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.itangcent.idea.plugin.api.export.ExportChannel
import com.itangcent.idea.plugin.api.export.ExportDoc
import com.itangcent.idea.plugin.api.export.core.*
import com.itangcent.idea.plugin.api.export.ecsb.*
import com.itangcent.idea.plugin.settings.helper.EcsbTokenChecker
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.file.DefaultLocalFileRepository
import com.itangcent.intellij.file.LocalFileRepository
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.suv.http.ConfigurableHttpClientProvider
import com.itangcent.suv.http.HttpClientProvider

class EcsbExportAction : ApiExportAction("Export Ecsb") {

    override fun afterBuildActionContext(event: AnActionEvent, builder: ActionContext.ActionContextBuilder) {
        super.afterBuildActionContext(event, builder)

        builder.bind(LocalFileRepository::class) { it.with(DefaultLocalFileRepository::class).singleton() }

        builder.bind(HttpClientProvider::class) { it.with(ConfigurableHttpClientProvider::class).singleton() }
        builder.bind(LinkResolver::class) { it.with(EcsbLinkResolver::class).singleton() }

        builder.bind(EcsbApiHelper::class) { it.with(EcsbCachedApiHelper::class).singleton() }

        builder.bind(ClassExporter::class) { it.with(CompositeClassExporter::class).singleton() }

        builder.bindInstance(ExportChannel::class, ExportChannel.of("ecsb"))
        builder.bindInstance(ExportDoc::class, ExportDoc.of("request", "methodDoc"))

        builder.bind(RequestBuilderListener::class) { it.with(CompositeRequestBuilderListener::class).singleton() }
        builder.bind(MethodDocBuilderListener::class) { it.with(CompositeMethodDocBuilderListener::class).singleton() }

        builder.bind(MethodFilter::class) { it.with(ConfigurableMethodFilter::class).singleton() }

        builder.bindInstance("file.save.default", "ecsb.json")
        builder.bindInstance("file.save.last.location.key", "com.itangcent.ecsb.export.path")

        builder.bind(PsiClassHelper::class) { it.with(EcsbPsiClassHelper::class).singleton() }

        builder.bind(EcsbTokenChecker::class) { it.with(EcsbTokenCheckerSupport::class).singleton() }

        builder.bind(AdditionalParseHelper::class) { it.with(EcsbAdditionalParseHelper::class).singleton() }
    }

    override fun actionPerformed(actionContext: ActionContext, project: Project?, anActionEvent: AnActionEvent) {
        super.actionPerformed(actionContext, project, anActionEvent)
        actionContext.instance(EcsbApiExporter::class).export()
    }

}