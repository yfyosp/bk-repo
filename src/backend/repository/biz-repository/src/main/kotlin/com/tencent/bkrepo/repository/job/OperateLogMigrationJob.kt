/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.repository.dao.repository.OperateLogRepository
import com.tencent.bkrepo.repository.job.base.CenterNodeJob
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OperateLogMigrationJob(
    private val operateLogRepository: OperateLogRepository,
    private val mongoTemplate: MongoTemplate
) : CenterNodeJob() {

    private val migrationIntervalDay = 1L

    @Scheduled(cron = "0 * * * * ?")
    override fun start() {
        super.start()
    }

    override fun run() {
        val migrationFromTime = LocalDate.now().minusDays(2 * migrationIntervalDay).atTime(0, 0)
        val migrationToTime = LocalDate.now().minusDays(migrationIntervalDay).atTime(0, 0)
        val operationLogs = operateLogRepository
            .findByCreatedDateBetween(migrationFromTime, migrationToTime)
        val insertedLogsNum = try {
            val insertedLogs = mongoTemplate.insert(operationLogs, "artifact_oplog_backup")
            insertedLogs.size
        } catch (ignore: DuplicateKeyException) {
            0
        }
        val deletedNum = operateLogRepository.deleteByCreatedDateBefore(migrationFromTime)
        logger.info("OperateLogMigrationJob finish, " +
            "migrate $insertedLogsNum logs createdDate between $migrationFromTime and $migrationToTime, " +
            "delete $deletedNum logs created before $migrationToTime.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OperateLogMigrationJob::class.java)
    }
}
