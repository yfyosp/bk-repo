/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.analyst.statemachine.task.action

import com.alibaba.cola.statemachine.StateMachine
import com.tencent.bkrepo.analyst.component.ScannerPermissionCheckHandler
import com.tencent.bkrepo.analyst.dao.ScanPlanDao
import com.tencent.bkrepo.analyst.dao.ScanTaskDao
import com.tencent.bkrepo.analyst.event.listener.ScanTaskStatusChangedEventListener
import com.tencent.bkrepo.analyst.message.ScannerMessageCode
import com.tencent.bkrepo.analyst.metrics.ScannerMetrics
import com.tencent.bkrepo.analyst.model.TScanPlan
import com.tencent.bkrepo.analyst.model.TScanTask
import com.tencent.bkrepo.analyst.pojo.ScanTask
import com.tencent.bkrepo.analyst.pojo.ScanTaskStatus
import com.tencent.bkrepo.analyst.pojo.ScanTriggerType
import com.tencent.bkrepo.analyst.pojo.TaskMetadata
import com.tencent.bkrepo.analyst.pojo.request.ScanRequest
import com.tencent.bkrepo.analyst.service.ScannerService
import com.tencent.bkrepo.analyst.statemachine.Action
import com.tencent.bkrepo.analyst.statemachine.task.ScanTaskEvent
import com.tencent.bkrepo.analyst.statemachine.task.context.CreateTaskContext
import com.tencent.bkrepo.analyst.statemachine.task.context.SubmitTaskContext
import com.tencent.bkrepo.analyst.statemachine.task.context.TaskContext
import com.tencent.bkrepo.analyst.task.ScanTaskSchedulerConfiguration
import com.tencent.bkrepo.analyst.utils.Converter
import com.tencent.bkrepo.analyst.utils.RuleConverter
import com.tencent.bkrepo.analyst.utils.RuleUtil
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils.getLocalizedMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Action
@Suppress("LongParameterList")
class PendingAction(
    private val scanPlanDao: ScanPlanDao,
    private val permissionCheckHandler: ScannerPermissionCheckHandler,
    private val scannerService: ScannerService,
    private val scanTaskDao: ScanTaskDao,
    private val scannerMetrics: ScannerMetrics,
    private val scanTaskStatusChangedEventListener: ScanTaskStatusChangedEventListener,
    @Qualifier(ScanTaskSchedulerConfiguration.SCAN_TASK_SCHEDULER_THREAD_POOL_BEAN_NAME)
    private val executor: ThreadPoolTaskExecutor
) : TaskAction {
    @Autowired
    @Lazy
    private lateinit var taskStateMachine: StateMachine<ScanTaskStatus, ScanTaskEvent, TaskContext>

    @Transactional(rollbackFor = [Throwable::class])
    override fun execute(from: ScanTaskStatus, to: ScanTaskStatus, event: ScanTaskEvent, context: TaskContext) {
        require(context is CreateTaskContext)
        with(context) {
            val task = createTask(scanRequest, triggerType, userId)
            weworkBotUrl?.let { scanTaskStatusChangedEventListener.setWeworkBotUrl(task.taskId, it, chatIds) }
            // 开始调度扫描任务
            executor.execute {
                taskStateMachine.fireEvent(ScanTaskStatus.PENDING, ScanTaskEvent.SUBMIT, SubmitTaskContext(task))
            }
            context.createdScanTask = task
        }
    }

    private fun createTask(scanRequest: ScanRequest, triggerType: ScanTriggerType, userId: String?): ScanTask {
        with(scanRequest) {
            if (planId == null && (scanner == null || rule == null)) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
            }

            val plan = planId?.let { scanPlanDao.get(it) }
            val projectId = projectId(rule, plan)
            val repoNames = RuleUtil.getRepoNames(rule)

            // 校验权限
            if (userId != null) {
                if (repoNames.isEmpty()) {
                    permissionCheckHandler.checkProjectPermission(projectId, PermissionAction.MANAGE, userId)
                } else {
                    permissionCheckHandler.checkReposPermission(projectId, repoNames, PermissionAction.READ, userId)
                }
            }

            val rule = RuleConverter.convert(rule, plan?.type, projectId)
            val scanner = scannerService.get(scanner ?: plan!!.scanner)
            val now = LocalDateTime.now()
            val scanTask = scanTaskDao.save(
                TScanTask(
                    createdBy = userId ?: SecurityUtils.getUserId(),
                    createdDate = now,
                    lastModifiedBy = userId ?: SecurityUtils.getUserId(),
                    lastModifiedDate = now,
                    name = scanTaskName(triggerType, metadata),
                    rule = rule.toJsonString(),
                    triggerType = triggerType.name,
                    planId = plan?.id,
                    projectId = projectId,
                    status = ScanTaskStatus.PENDING.name,
                    total = 0L,
                    scanning = 0L,
                    failed = 0L,
                    scanned = 0L,
                    passed = 0L,
                    scanner = scanner.name,
                    scannerType = scanner.type,
                    scannerVersion = scanner.version,
                    scanResultOverview = emptyMap(),
                    metadata = metadata
                )
            ).run { Converter.convert(this, plan, force) }
            plan?.id?.let { scanPlanDao.updateLatestScanTaskId(it, scanTask.taskId) }
            scannerMetrics.incTaskCountAndGet(ScanTaskStatus.PENDING)
            logger.info("create scan task[${scanTask.taskId}] success")
            return scanTask
        }
    }

    private fun scanTaskName(triggerType: ScanTriggerType, metadata: List<TaskMetadata>): String {
        return when (triggerType) {
            ScanTriggerType.PIPELINE -> {
                val metadataMap = metadata.associateBy { it.key }
                val pipelineName = metadataMap[TaskMetadata.TASK_METADATA_PIPELINE_NAME]?.value ?: ""
                val buildNo = metadataMap[TaskMetadata.TASK_METADATA_BUILD_NUMBER]?.value ?: ""
                val pluginName = metadataMap[TaskMetadata.TASK_METADATA_PLUGIN_NAME]?.value ?: ""
                "$pipelineName-$buildNo-$pluginName"
            }

            ScanTriggerType.MANUAL_SINGLE -> getLocalizedMessage(ScannerMessageCode.SCAN_TASK_NAME_SINGLE_SCAN)
            else -> getLocalizedMessage(ScannerMessageCode.SCAN_TASK_NAME_BATCH_SCAN)
        }
    }

    private fun projectId(rule: Rule?, plan: TScanPlan?): String {
        // 尝试从rule取projectId，不存在时从plan中取projectId
        val projectIds = RuleUtil.getProjectIds(rule)
        return if (projectIds.size == 1) {
            projectIds.first()
        } else if (projectIds.isEmpty() && plan != null) {
            plan.projectId
        } else {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
        }
    }

    override fun support(from: ScanTaskStatus, to: ScanTaskStatus, event: ScanTaskEvent): Boolean {
        return from == ScanTaskStatus.PENDING && to == ScanTaskStatus.PENDING && event == ScanTaskEvent.CREATE
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PendingAction::class.java)
    }
}
