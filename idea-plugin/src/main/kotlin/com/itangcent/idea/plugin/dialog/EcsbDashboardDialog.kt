package com.itangcent.idea.plugin.dialog

import com.google.inject.Inject
import com.intellij.openapi.ui.Messages
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.model.Doc
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.idea.icons.EasyIcons
import com.itangcent.idea.icons.iconOnly
import com.itangcent.idea.plugin.api.export.ecsb.EcsbApiDashBoardExporter
import com.itangcent.idea.plugin.api.export.ecsb.EcsbApiHelper
import com.itangcent.idea.plugin.api.export.ecsb.EcsbFormatter
import com.itangcent.idea.plugin.settings.helper.EcsbSettingsHelper
import com.itangcent.idea.plugin.support.IdeaSupport
import com.itangcent.idea.swing.EasyApiTreeCellRenderer
import com.itangcent.idea.swing.IconCustomized
import com.itangcent.idea.swing.ToolTipAble
import com.itangcent.idea.utils.SwingUtils
import com.itangcent.idea.utils.isDoubleClick
import com.itangcent.idea.utils.reload
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.*
import com.itangcent.intellij.extend.rx.from
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class EcsbDashboardDialog : AbstractApiDashboardDialog() {
    override var contentPane: JPanel? = null
    override var projectApiTree: JTree? = null
    private var ecsbApiTree: JTree? = null
    override var projectApiPanel: JPanel? = null
    private var ecsbPanel: JPanel? = null
    override var projectApiModeButton: JButton? = null
    override var projectCollapseButton: JButton? = null

    private var ecsbNewProjectButton: JButton? = null
    private var ecsbSyncButton: JButton? = null
    private var ecsbCollapseButton: JButton? = null

    private var ecsbPopMenu: JPopupMenu? = null

    private var projectMode: ProjectMode = ProjectMode.Legible

    @Inject
    private lateinit var ecsbApiHelper: EcsbApiHelper

    @Inject
    private val ecsbApiDashBoardExporter: EcsbApiDashBoardExporter? = null

    @Inject
    protected lateinit var ecsbSettingsHelper: EcsbSettingsHelper

    init {
        setContentPane(contentPane)
        isModal = true
    }

    override fun init() {
        super.init()

        EasyIcons.CollapseAll.iconOnly(this.projectCollapseButton)
        EasyIcons.CollapseAll.iconOnly(this.ecsbCollapseButton)
        EasyIcons.Add.iconOnly(this.ecsbNewProjectButton)
        EasyIcons.Refresh.iconOnly(this.ecsbSyncButton)

        try {
            val projectCellRenderer = EasyApiTreeCellRenderer()

            this.projectApiTree!!.cellRenderer = projectCellRenderer

            projectCellRenderer.leafIcon = EasyIcons.Method
            projectCellRenderer.openIcon = EasyIcons.WebFolder
            projectCellRenderer.closedIcon = EasyIcons.WebFolder

            val ecsbCellRenderer = EasyApiTreeCellRenderer()

            this.ecsbApiTree!!.cellRenderer = ecsbCellRenderer

            ecsbCellRenderer.leafIcon = EasyIcons.Link
            ecsbCellRenderer.openIcon = EasyIcons.WebFolder
            ecsbCellRenderer.closedIcon = EasyIcons.WebFolder

        } catch (_: Exception) {
        }

        ecsbPopMenu = JPopupMenu()

        val addItem = JMenuItem("Add Cart")

        addItem.addActionListener {
            newEcsbCart()
        }

        val unloadItem = JMenuItem("Unload")

        unloadItem.addActionListener {
            unloadEcsbProject()
        }

        val syncItem = JMenuItem("Sync")

        syncItem.addActionListener {
            try {
                syncEcsbProject()
            } catch (e: Exception) {
                logger.traceError("sync failed", e)
            }
        }

        val curlItem = JMenuItem("Copy Curl")
        curlItem.addActionListener {
            selectedEcsbNode()?.let { copyCurl(it) }
        }

        ecsbPopMenu!!.add(addItem)
        ecsbPopMenu!!.add(unloadItem)
        ecsbPopMenu!!.add(syncItem)
        ecsbPopMenu!!.add(curlItem)

        this.ecsbApiTree!!.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (e == null) return
                if (SwingUtilities.isRightMouseButton(e)) {
                    val path = ecsbApiTree!!.getPathForLocation(e.x, e.y) ?: return

                    val targetComponent = path.lastPathComponent
                    val ecsbNodeData = (targetComponent as DefaultMutableTreeNode).userObject

                    addItem.isEnabled = ecsbNodeData is EcsbProjectNodeData
                    unloadItem.isEnabled = ecsbNodeData is EcsbProjectNodeData
                    ecsbPopMenu!!.show(ecsbApiTree!!, e.x, e.y)
                    ecsbApiTree!!.selectionPath = path
                }
            }

            override fun mouseClicked(e: MouseEvent?) {
                if (e.isDoubleClick()) {
                    e?.consume()
                }
            }
        })

        ecsbApiTree!!.model = null

        autoComputer.bindEnable(this.ecsbCollapseButton!!)
            .from(this::ecsbAvailable)
        autoComputer.bindEnable(this.ecsbSyncButton!!)
            .from(this::ecsbAvailable)
        autoComputer.bindEnable(this.ecsbNewProjectButton!!)
            .from(this::ecsbAvailable)


        //drop drag from api to ecsb
        DropTarget(this.ecsbApiTree, DnDConstants.ACTION_COPY_OR_MOVE, object : DropTargetAdapter() {

            override fun drop(dtde: DropTargetDropEvent?) {
                if (dtde == null) return


                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)

                    val tp: TreePath = ecsbApiTree!!.getPathForLocation(dtde.location.x, dtde.location.y) ?: return
                    val targetComponent = tp.lastPathComponent
                    val ecsbNodeData = (targetComponent as DefaultMutableTreeNode).userObject

                    val transferable = dtde.transferable
                    val wrapDataFlavor = getWrapDataFlavor()

                    val transferData = transferable.getTransferData(wrapDataFlavor)

                    val projectNodeData =
                        (transferData as WrapData).wrapHash?.let { safeHashHelper.getBean(it) } as? ProjectNodeData
                            ?: return

                    handleDropEvent(projectNodeData, ecsbNodeData)

                } catch (e: java.lang.Exception) {
                    logger.traceWarn("drop failed", e)
                } finally {
                    dtde.dropComplete(true)
                }
            }
        })

        this.ecsbCollapseButton!!.addActionListener {
            try {
                SwingUtils.expandOrCollapseNode(this.ecsbApiTree!!, false)
            } catch (e: Exception) {
                logger.error("try collapse ecsb apis failed!")
            }
        }

        this.ecsbSyncButton!!.addActionListener {
            ((this.ecsbApiTree!!.model as DefaultTreeModel).root as DefaultMutableTreeNode).removeAllChildren()
            loadEcsbInfo()
        }

        this.ecsbNewProjectButton!!.addActionListener {
            importNewEcsbProject()
        }

        initEcsbInfo()
    }

    //region ecsb module-----------------------------------------------------

    private var ecsbAvailable: Boolean = true

    private fun initEcsbInfo() {
        actionContext.runWithContext {
            if (ecsbSettingsHelper.hasServer()) {
                loadEcsbInfo()
            } else {
                autoComputer.value(this::ecsbAvailable, false)
                actionContext.runAsync {
                    if (ecsbSettingsHelper.getServer(false).notNullOrBlank()) {
                        autoComputer.value(this::ecsbAvailable, true)
                        loadEcsbInfo()
                    }
                }
            }
        }
    }

    private fun loadEcsbInfo() {
        actionContext.runInNormalThread {
            if (!ecsbSettingsHelper.hasServer()) {
                actionContext.runInSwingUI {
                    Messages.showErrorDialog(
                        this,
                        "Load ecsb info failed, no server be found", "Error"
                    )
                }
                return@runInNormalThread
            }

            actionContext.runInSwingUI {
                //            ecsbApiTree!!.dragEnabled = true
                val treeNode = DefaultMutableTreeNode()
                val rootTreeModel = DefaultTreeModel(treeNode, true)

                actionContext.runAsync {
                    var projectNodes: ArrayList<EcsbProjectNodeData>? = null
                    try {
                        val ecsbTokens = ecsbSettingsHelper.readTokens()

                        if (ecsbTokens.isEmpty()) {
                            actionContext.runInSwingUI {
                                Messages.showErrorDialog(
                                    this,
                                    "No token be found", "Error"
                                )
                            }
                            return@runAsync
                        }

                        projectNodes = ArrayList()

                        ecsbTokens.values.asSequence().distinct().forEach { token ->

                            logger.info("load token:$token")
                            val projectId = ecsbApiHelper.getProjectIdByToken(token)
                            if (projectId.isNullOrBlank()) {
                                return@forEach
                            }

                            val projectInfo = ecsbApiHelper.getProjectInfo(token, projectId)
                                .sub("data")
                                ?.asMap()

                            if (projectInfo.isNullOrEmpty()) {
                                logger.info("invalid token:$token")
                                return@forEach
                            }

                            val projectNode = EcsbProjectNodeData(token, projectInfo)
                            treeNode.add(projectNode.asTreeNode())
                            projectNodes.add(projectNode)
                            actionContext.runInSwingUI {
                                rootTreeModel.reload(projectNode.asTreeNode())
                            }
                        }
                    } catch (e: Exception) {
                        logger.traceError("error to load ecsb info", e)
                    }

                    actionContext.runInSwingUI {
                        ecsbApiTree!!.model = rootTreeModel

                        actionContext.runAsync {
                            try {
                                if (projectNodes != null) {
                                    val boundary = actionContext.createBoundary()
                                    try {
                                        for (projectNode in projectNodes) {
                                            Thread.sleep(200)
                                            loadEcsbProject(projectNode)
                                            boundary.waitComplete(false)
                                        }
                                    } finally {
                                        boundary.remove()
                                    }
                                }
                            } catch (_: InterruptedException) {
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadEcsbProject(projectNode: EcsbProjectNodeData) {
        val projectId = projectNode.getProjectId()
        val ecsbApiTreeModel = ecsbApiTree!!.model as DefaultTreeModel
        if (projectId == null) {
            actionContext.runInSwingUI {
                projectNode.removeFromParent()
                ecsbApiTreeModel.reload(projectNode.asTreeNode())
            }
            return
        }

        actionContext.runAsync {
            projectNode.status = NodeStatus.Loading
            actionContext.withBoundary {
                val carts = ecsbApiHelper.findCarts(projectId.toString(), projectNode.getProjectToken())
                if (carts.isNullOrEmpty()) {
                    return@withBoundary
                }
                for (cart in carts) {
                    val ecsbCartNode = EcsbCartNodeData(cart as HashMap<String, Any?>)
                    projectNode.addSubNodeData(ecsbCartNode)
                }
                projectNode.getSubNodeData()?.forEach {
                    (it as? EcsbCartNodeData)?.let { cart -> loadEcsbCart(cart) }
                }
            }
            projectNode.status = NodeStatus.Loaded
            actionContext.runInSwingUI {
                ecsbApiTreeModel.reload(projectNode.asTreeNode())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadEcsbCart(ecsbCartNodeData: EcsbCartNodeData) {

        actionContext.runInSwingUI {
            val cartInfo = ecsbCartNodeData.info

            val apis = ecsbApiHelper.findApis(
                ecsbCartNodeData.getProjectToken(),
                cartInfo["_id"].toString()
            )
            if (apis.isNullOrEmpty()) return@runInSwingUI
            for (api in apis) {
                loadEcsbApi(ecsbCartNodeData, api as HashMap<String, Any?>)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadEcsbApi(parentNode: EcsbCartNodeData, item: HashMap<String, Any?>) {
        if (item.isNullOrEmpty()) return
        actionContext.runInSwingUI {
            val apiNodeData = EcsbApiNodeData(parentNode, item)
            parentNode.addSubNodeData(apiNodeData)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun importNewEcsbProject() {
        actionContext.runAsync {
            val projectToken = this.ecsbSettingsHelper.inputNewToken()

            if (projectToken.isNullOrBlank()) return@runAsync

            val projectId = ecsbApiHelper.getProjectIdByToken(projectToken)

            if (projectId.isNullOrEmpty()) {
                return@runAsync
            }

            val projectInfo = ecsbApiHelper.getProjectInfo(projectToken, projectId)
                .sub("data")
                ?.asMap()

            if (projectInfo.isNullOrEmpty()) {
                logger.error("invalid token:$projectToken")
                return@runAsync
            }

            actionContext.runInSwingUI {
                val ecsbProjectName = projectInfo["name"].toString()
                val moduleName = Messages.showInputDialog(
                    this,
                    "Input Module Name Of Project",
                    "Module Name",
                    Messages.getInformationIcon(),
                    ecsbProjectName,
                    null
                )

                @Suppress("LABEL_NAME_CLASH")
                if (moduleName.isNullOrBlank()) return@runInSwingUI

                actionContext.runAsync {

                    ecsbSettingsHelper.setToken(moduleName, projectToken)
                    actionContext.runInSwingUI {
                        val projectTreeNode = EcsbProjectNodeData(projectToken, projectInfo)
                        var model = ecsbApiTree!!.model
                        if (model == null) {
                            val treeNode = DefaultMutableTreeNode()
                            model = DefaultTreeModel(treeNode, true)
                            ecsbApiTree!!.model = model
                        }

                        val ecsbTreeModel = model as DefaultTreeModel

                        (ecsbTreeModel.root as DefaultMutableTreeNode).add(projectTreeNode.asTreeNode())
                        ecsbTreeModel.reload()

                        loadEcsbProject(projectTreeNode)
                    }
                }
            }
        }
    }

    //endregion ecsb module-----------------------------------------------------

    //region ecsb pop action---------------------------------------------------------

    private fun newEcsbCart() {
        val lastSelectedPathComponent = ecsbApiTree!!.lastSelectedPathComponent as (DefaultMutableTreeNode?)

        if (lastSelectedPathComponent != null) {
            val ecsbNodeData = lastSelectedPathComponent.userObject as EcsbNodeData

            actionContext.runInSwingUI {
                val cartName = Messages.showInputDialog(
                    this,
                    "Input Cart Name",
                    "Cart Name",
                    Messages.getInformationIcon()
                )
                if (cartName.isNullOrBlank()) return@runInSwingUI

                ecsbApiHelper.addCart(ecsbNodeData.getProjectToken()!!, cartName, "")

                syncEcsbNode(ecsbNodeData.getRootNodeData())
            }
        }
    }

    private fun unloadEcsbProject() {
        val lastSelectedPathComponent = ecsbApiTree!!.lastSelectedPathComponent as (DefaultMutableTreeNode?)

        if (lastSelectedPathComponent != null) {
            val ecsbNodeData = lastSelectedPathComponent.userObject as EcsbNodeData

            ecsbSettingsHelper.removeToken(ecsbNodeData.getProjectToken()!!)

            val treeModel = ecsbApiTree!!.model as DefaultTreeModel
            (treeModel.root as DefaultMutableTreeNode)
                .remove(ecsbNodeData.getRootNodeData().asTreeNode())
            treeModel.reload()
        }

    }

    private fun syncEcsbProject() {
        val ecsbNodeData = selectedEcsbNode() ?: return
        logger.info("reload:[$ecsbNodeData]")
        syncEcsbNode(ecsbNodeData)
    }

    private fun syncEcsbNode(ecsbNodeData: EcsbNodeData) {
        actionContext.runInSwingUI {
            when (ecsbNodeData) {
                is EcsbApiNodeData -> {
                    syncEcsbNode(ecsbNodeData.getParentNodeData())
                }

                is EcsbProjectNodeData -> {
                    //clear
                    ecsbNodeData.removeAllSub()
                    //reload
                    loadEcsbProject(ecsbNodeData)
                }

                is EcsbCartNodeData -> {
                    //clear
                    ecsbNodeData.removeAllSub()
                    loadEcsbCart(ecsbNodeData)
                    ecsbApiTree!!.model.reload(ecsbNodeData.getRootNodeData().asTreeNode())
                }
            }
        }
    }

    private fun selectedEcsbNode(): EcsbNodeData? {
        return (ecsbApiTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode)
            ?.userObject as? EcsbNodeData
    }

    //endregion ecsb pop action---------------------------------------------------------

    //region ecsb Node Data--------------------------------------------------

    abstract class EcsbNodeData : DocContainer, TreeNodeData<EcsbNodeData>() {
        abstract fun data(): Map<String, Any?>

        abstract fun getProjectId(): String?

        abstract fun getProjectToken(): String?

        abstract fun getUrl(ecsbDashboardDialog: EcsbDashboardDialog): String?

        override fun docs(handle: (Doc) -> Unit) {
            this.getSubNodeData()?.forEach { it.docs(handle) }
        }
    }

    class EcsbProjectNodeData(
        private var projectToken: String,
        var projectInfo: Map<String, Any?>,
    ) : EcsbNodeData(), IconCustomized {
        override fun icon(): Icon? {
            return when (status) {
                NodeStatus.Loading -> EasyIcons.Refresh
                NodeStatus.Uploading -> EasyIcons.UpFolder
                else -> null
            } ?: EasyIcons.ModuleGroup
        }

        override fun data(): Map<String, Any?> {
            return projectInfo
        }

        override fun getParentNodeData(): EcsbNodeData? {
            return null
        }

        var status = NodeStatus.Unload

        override fun getProjectId(): String? {
            return projectInfo["_id"]?.toString()
        }

        override fun getProjectToken(): String {
            return projectToken
        }

        override fun getUrl(ecsbDashboardDialog: EcsbDashboardDialog): String? {
            if (!ecsbDashboardDialog.ecsbSettingsHelper.hasServer()) {
                return null
            }
            val projectId = getProjectId() ?: return null
            return "${ecsbDashboardDialog.ecsbSettingsHelper.getServer(true)}/project/${projectId}/interface/api"
        }

        override fun toString(): String {
            return status.desc + projectInfo.getOrDefault("name", "unknown")
        }
    }

    class EcsbCartNodeData(
        var info: HashMap<String, Any?>,
    ) : EcsbNodeData(), IconCustomized, ToolTipAble {

        override fun icon(): Icon? {
            return EasyIcons.Module
        }

        override fun data(): HashMap<String, Any?> {
            return info
        }

        override fun getParentNodeData(): EcsbProjectNodeData {
            return super.getParentNodeData() as EcsbProjectNodeData
        }

        override fun getProjectId(): String? {
            return getParentNodeData().getProjectId()
        }

        override fun getProjectToken(): String {
            return getParentNodeData().getProjectToken()
        }

        override fun getUrl(ecsbDashboardDialog: EcsbDashboardDialog): String? {
            if (!ecsbDashboardDialog.ecsbSettingsHelper.hasServer()) {
                return null
            }
            val projectId = getProjectId() ?: return null
            val cartId = info["_id"]?.toString() ?: return null
            return "${ecsbDashboardDialog.ecsbSettingsHelper.getServer(true)}/project/${projectId}/interface/api/cat_${cartId}"
        }

        override fun toString(): String {
            return info.getOrDefault("name", "unknown").toString()
        }

        override fun toolTip(): String? {
            return info["desc"]?.toString()
        }
    }

    class EcsbApiNodeData(private var parentNode: EcsbCartNodeData, var info: HashMap<String, Any?>) : EcsbNodeData(),
        IconCustomized, ToolTipAble {

        private val ecsbFormatter: EcsbFormatter by lazy {
            ActionContext.getContext()!!.instance(EcsbFormatter::class)
        }

        override fun icon(): Icon? {
            return EasyIcons.Link
        }

        override fun data(): HashMap<String, Any?> {
            return info
        }

        override fun getParentNodeData(): EcsbCartNodeData {
            return parentNode
        }

        override fun getProjectId(): String? {
            return parentNode.getProjectId()
        }

        override fun getProjectToken(): String {
            return parentNode.getProjectToken()
        }

        override fun docs(handle: (Doc) -> Unit) {
            handle(ecsbFormatter.item2Request(this.info))
        }

        override fun asTreeNode(): DefaultMutableTreeNode {
            return super.asTreeNode().also { it.allowsChildren = false }
        }

        override fun toString(): String {
            return info.getOrDefault("title", "unknown").toString()
        }

        override fun toolTip(): String {
            val sb = StringBuilder()
            val method = info["method"]
            if (method != null) {
                sb.append(method).append(":")
            }
            val path = info["path"]
            if (path != null) {
                sb.append(path)
            }
            return sb.toString()
        }

        override fun getUrl(ecsbDashboardDialog: EcsbDashboardDialog): String? {
            if (!ecsbDashboardDialog.ecsbSettingsHelper.hasServer()) {
                return null
            }
            val projectId = getProjectId() ?: return null
            val apiId = info["_id"]?.toString() ?: return null
            return "${ecsbDashboardDialog.ecsbSettingsHelper.getServer(true)}/project/${projectId}/interface/api/${apiId}"
        }

    }

    //endregion ecsb Node Data--------------------------------------------------

    //region handle drop--------------------------------------------------------

    @Suppress("UNCHECKED_CAST", "LABEL_NAME_CLASH")
    fun handleDropEvent(fromProjectData: ProjectNodeData, toEcsbNodeData: Any) {

        //    \to  | api    |cart               |project
        // from\   |        |                   |
        // api     |see ->  |✔️                 |new cart
        // class   |see ->  |new cart/the cart  |new cart
        // module  |see ->  |new cart/the cart  |new cart

        //targetNodeData should be EcsbCartNodeData or EcsbProjectNodeData
        val targetNodeData: EcsbNodeData = when (toEcsbNodeData) {
            is EcsbApiNodeData -> toEcsbNodeData.getParentNodeData()
            else -> toEcsbNodeData as EcsbNodeData
        }

        logger.info("export [$fromProjectData] to $targetNodeData")

        actionContext.runAsync {
            try {

                logger.info("parse api...")

                if (targetNodeData is EcsbCartNodeData) {
                    if (fromProjectData is ApiProjectNodeData) {
                        export(fromProjectData, targetNodeData)
                        Thread.sleep(200)
                        syncEcsbNode(targetNodeData)
                    } else {
                        actionContext.runInSwingUI {
                            val yesNoCancel = Messages.showYesNoCancelDialog(
                                project!!, "Add as new cart?",
                                "Export", "New", "Not", "Cancel", Messages.getInformationIcon()
                            )
                            actionContext.runAsync {
                                when (yesNoCancel) {
                                    Messages.CANCEL -> return@runAsync
                                    Messages.OK -> {
                                        export(fromProjectData, targetNodeData)
                                        Thread.sleep(200)
                                        syncEcsbNode(targetNodeData.getRootNodeData())
                                    }

                                    Messages.NO -> {
                                        export(fromProjectData, targetNodeData)
                                        Thread.sleep(200)
                                        syncEcsbNode(targetNodeData.getRootNodeData())
                                    }
                                }
                            }
                        }
                    }
                } else {
                    export(fromProjectData, targetNodeData)
                    Thread.sleep(200)
                    syncEcsbNode(targetNodeData.getRootNodeData())
                }
            } catch (e: Exception) {
                logger.traceError("export failed", e)
            }
        }
    }

    private fun export(
        fromProjectData: ProjectNodeData, targetNodeData: EcsbNodeData,
    ) {

        val privateToken = targetNodeData.getProjectToken()
        if (privateToken == null) {
            logger.error("target token missing!Please try sync")
            return
        }

        val docHandle: (Doc) -> Unit = { doc -> ecsbApiDashBoardExporter!!.exportDoc(doc, privateToken) }

        fromProjectData.docs(docHandle)
        logger.info("exported success")
    }

    //endregion handle drop--------------------------------------------------------
    companion object : Log()
}