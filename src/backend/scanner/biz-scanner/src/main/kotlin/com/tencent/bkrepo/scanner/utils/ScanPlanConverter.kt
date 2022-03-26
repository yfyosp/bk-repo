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

@file:Suppress("DEPRECATION")

package com.tencent.bkrepo.scanner.utils

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.scanner.pojo.scanner.binauditor.BinAuditorScanExecutorResult
import com.tencent.bkrepo.common.scanner.pojo.scanner.binauditor.BinAuditorScanner
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.LEVEL_CRITICAL
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.LEVEL_HIGH
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.LEVEL_LOW
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.LEVEL_MID
import com.tencent.bkrepo.common.scanner.pojo.scanner.utils.normalizedLevel
import com.tencent.bkrepo.scanner.model.SubScanTaskDefinition
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.model.TScanTask
import com.tencent.bkrepo.scanner.pojo.PlanType
import com.tencent.bkrepo.scanner.pojo.ScanPlan
import com.tencent.bkrepo.scanner.pojo.ScanStatus
import com.tencent.bkrepo.scanner.pojo.ScanTaskStatus
import com.tencent.bkrepo.scanner.pojo.request.CreateScanPlanRequest
import com.tencent.bkrepo.scanner.pojo.request.PlanArtifactRequest
import com.tencent.bkrepo.scanner.pojo.request.UpdateScanPlanRequest
import com.tencent.bkrepo.scanner.pojo.response.ArtifactPlanRelation
import com.tencent.bkrepo.scanner.pojo.response.PlanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.response.ScanPlanBase
import com.tencent.bkrepo.scanner.pojo.response.ScanPlanInfo
import java.time.Duration
import java.time.format.DateTimeFormatter

object ScanPlanConverter {
    fun convert(scanPlan: TScanPlan): ScanPlan {
        return with(scanPlan) {
            ScanPlan(
                id = id!!,
                projectId = projectId,
                name = name,
                type = type,
                scanner = scanner,
                description = description,
                scanOnNewArtifact = scanOnNewArtifact,
                repoNames = repoNames,
                rule = rule.readJsonString(),
                createdBy = createdBy,
                createdDate = createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun convert(scanPlan: ScanPlan): ScanPlanBase {
        return with(scanPlan) {
            ScanPlanBase(
                id = id!!,
                name = name,
                type = type!!,
                scanner = scanner!!,
                description = description!!,
                projectId = projectId!!,
                autoScan = scanOnNewArtifact!!,
                scanOnNewArtifact = scanOnNewArtifact!!,
                repoNameList = repoNames!!,
                repoNames = repoNames!!,
                artifactRules = rule?.let { RuleConverter.convert(it) } ?: emptyList(),
                rule = rule,
                createdBy = createdBy!!,
                createdDate = createdDate!!,
                lastModifiedBy = lastModifiedBy!!,
                lastModifiedDate = lastModifiedDate!!
            )
        }
    }

    fun convert(scanPlanRequest: UpdateScanPlanRequest, curRepoNames: List<String>, curRule: Rule): ScanPlan {
        return with(scanPlanRequest) {
            val rule = if (repoNameList?.isEmpty() == true && artifactRules?.isEmpty() == true) {
                null
            } else {
                val repoNames = repoNameList ?: curRepoNames
                val artifactRules = artifactRules ?: RuleConverter.convert(curRule)
                RuleConverter.convert(projectId!!, repoNames, artifactRules)
            }
            ScanPlan(
                id = id,
                projectId = projectId,
                name = name,
                description = description,
                scanOnNewArtifact = autoScan,
                repoNames = repoNameList,
                rule = rule
            )
        }
    }

    fun convert(scanPlanRequest: CreateScanPlanRequest): ScanPlan {
        return with(scanPlanRequest) {
            ScanPlan(
                projectId = projectId,
                name = name,
                type = type,
                scanner = scanner,
                description = description,
                scanOnNewArtifact = autoScan,
                repoNames = repoNameList,
                rule = RuleConverter.convert(projectId, repoNameList, artifactRules)
            )
        }
    }

    fun convert(scanPlan: TScanPlan, latestScanTask: TScanTask?): ScanPlanInfo {
        with(scanPlan) {
            val critical = latestScanTask?.let { getCveCount(LEVEL_CRITICAL, latestScanTask) } ?: 0L
            val high = latestScanTask?.let { getCveCount(LEVEL_HIGH, latestScanTask) } ?: 0L
            val medium = latestScanTask?.let { getCveCount(LEVEL_MID, latestScanTask) } ?: 0L
            val low = latestScanTask?.let { getCveCount(LEVEL_LOW, latestScanTask) } ?: 0L
            val artifactCount = latestScanTask?.total ?: 0L
            val status = latestScanTask?.let { convertToScanStatus(it.status).name } ?: ScanStatus.INIT.name

            return ScanPlanInfo(
                id = id!!,
                name = name,
                planType = type,
                projectId = projectId,
                status = status,
                artifactCount = artifactCount,
                critical = critical,
                high = high,
                medium = medium,
                low = low,
                total = critical + high + medium + low,
                createdBy = createdBy,
                createdDate = createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastScanDate = latestScanTask?.startDateTime?.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun convert(request: PlanArtifactRequest): PlanArtifactRequest {
        request.highestLeakLevel = request.highestLeakLevel?.let { normalizedLevel(it) }
        if (!request.status.isNullOrEmpty()) {
            request.subScanTaskStatus = convertToSubScanTaskStatus(ScanStatus.valueOf(request.status!!)).map { it.name }
        }
        return request
    }

    fun convertToPlanArtifactInfo(subScanTask: SubScanTaskDefinition, createdBy: String): PlanArtifactInfo {
        return with(subScanTask) {
            val duration = if (startDateTime != null && finishedDateTime != null) {
                Duration.between(startDateTime, finishedDateTime).toMillis()
            } else {
                0L
            }
            PlanArtifactInfo(
                recordId = id!!,
                subTaskId = id!!,
                name = artifactName,
                packageKey = packageKey,
                version = version,
                fullPath = fullPath,
                repoType = repoType,
                repoName = repoName,
                highestLeakLevel = scanResultOverview?.let { highestLeakLevel(it) },
                duration = duration,
                finishTime = finishedDateTime?.format(DateTimeFormatter.ISO_DATE_TIME),
                status = convertToScanStatus(status).name,
                createdBy = createdBy,
                createdDate = createdDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun convertToArtifactPlanRelation(subScanTask: SubScanTaskDefinition): ArtifactPlanRelation {
        val planType = if (subScanTask.repoType == RepositoryType.GENERIC.name) {
            PlanType.MOBILE.name
        } else {
            PlanType.DEPENDENT.name
        }
        return with(subScanTask) {
            ArtifactPlanRelation(
                id = planId!!,
                planId = planId,
                projectId = projectId,
                planType = planType,
                name = artifactName,
                status = convertToScanStatus(status).name,
                recordId = id!!,
                subTaskId = id!!
            )
        }
    }

    fun artifactStatus(status: List<String>): String {
        require(status.isNotEmpty())
        var maxStatus: ScanStatus? = null
        status.forEach { curStatus ->
            if (curStatus == ScanStatus.RUNNING.name) {
                return curStatus
            }
            maxStatus = maxStatus
                ?.let { max -> maxOf(ScanStatus.valueOf(curStatus), max) }
                ?: ScanStatus.valueOf(curStatus)
        }
        return maxStatus!!.name
    }

    private fun highestLeakLevel(overview: Map<String, Number>): String {
        return if (overview.keys.contains(LEVEL_CRITICAL)) {
            LEVEL_CRITICAL
        } else if (overview.keys.contains(LEVEL_HIGH)) {
            LEVEL_HIGH
        } else if (overview.keys.contains(LEVEL_MID)) {
            LEVEL_MID
        } else {
            LEVEL_LOW
        }
    }

    private fun convertToSubScanTaskStatus(status: ScanStatus): List<SubScanTaskStatus> {
        return when (status) {
            ScanStatus.INIT -> listOf(SubScanTaskStatus.CREATED, SubScanTaskStatus.PULLED, SubScanTaskStatus.ENQUEUED)
            ScanStatus.RUNNING -> listOf(SubScanTaskStatus.EXECUTING)
            ScanStatus.STOP -> listOf(SubScanTaskStatus.STOP)
            ScanStatus.FAILED -> listOf(SubScanTaskStatus.FAILED)
            ScanStatus.SUCCESS -> listOf(SubScanTaskStatus.SUCCESS)
        }
    }

    private fun convertToScanStatus(status: String?): ScanStatus {
        return when (status) {
            SubScanTaskStatus.CREATED.name,
            SubScanTaskStatus.PULLED.name,
            SubScanTaskStatus.ENQUEUED.name,
            ScanTaskStatus.PENDING.name -> ScanStatus.INIT

            SubScanTaskStatus.EXECUTING.name,
            ScanTaskStatus.SCANNING_SUBMITTING.name,
            ScanTaskStatus.SCANNING_SUBMITTED.name -> ScanStatus.RUNNING

            SubScanTaskStatus.STOP.name,
            ScanTaskStatus.PAUSE.name,
            ScanTaskStatus.STOPPED.name -> ScanStatus.STOP

            SubScanTaskStatus.SUCCESS.name,
            ScanTaskStatus.FINISHED.name -> ScanStatus.SUCCESS

            SubScanTaskStatus.TIMEOUT.name,
            SubScanTaskStatus.FAILED.name -> ScanStatus.FAILED
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, status.toString())
        }
    }

    private fun getCveCount(level: String, scanTask: TScanTask): Long {
        if (scanTask.scannerType == BinAuditorScanner.TYPE) {
            val key = BinAuditorScanExecutorResult.overviewKeyOfCve(level)
            return scanTask.scanResultOverview?.get(key) ?: 0L
        }

        return 0L
    }
}
