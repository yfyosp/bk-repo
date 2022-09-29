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

package com.tencent.bkrepo.common.analysis.pojo.scanner.standard

/**
 * 分析工具输出
 * 1. 指定--output output.json参数时输出到本地
 * 2. 指定--token参数时直接通过接口上报分析管理服务
 */
data class ToolOutput(
    /**
     * 分析状态，SUCCESS,FAILED,TIMEOUT
     */
    val status: String,
    /**
     * 分析失败时的错误信息
     */
    val err: String? = null,
    /**
     * 子任务id，工具输入参数存在--taskId时需要设置
     */
    val taskId: String? = null,
    /**
     * 调用分析管理服务使用的临时token，工具输入存在--token时需要设置
     */
    val token: String? = null,
    /**
     * 分析结果
     */
    val result: Result? = null
)

/**
 * 分析结果
 */
data class Result(
    /**
     * 漏洞分析结果
     */
    val securityResults: List<SecurityResult>? = null,
    /**
     * License分析结果
     */
    val licenseResults: List<LicenseResult>? = null
)

/**
 * 漏洞分析结果
 */
data class SecurityResult(
    /**
     * 漏洞id
     */
    val vulId: String,
    /**
     * 漏洞名
     */
    val vulName: String? = null,
    val cveId: String? = null,
    /**
     * 漏洞在制品压缩包中的路径
     */
    val path: String? = null,
    /**
     * 存在漏洞的组件
     */
    val pkgName: String? = null,
    /**
     * 存在漏洞的组件版本
     */
    val pkgVersions: Set<String> = emptySet(),
    /**
     * 修复版本
     */
    val fixedVersion: String? = null,
    /**
     * 漏洞描述
     */
    val des: String? = null,
    /**
     * 解决方案
     */
    val solution: String? = null,
    /**
     * 引用
     */
    val references: List<String> = emptyList(),
    val cvss: Double? = null,
    /**
     * 漏洞等级LOW, MEDIUM, HIGH, CRITICAL
     */
    val severity: String
)

/**
 * License分析结果
 */
data class LicenseResult(
    /**
     * 许可证名
     */
    val licenseName: String,
    /**
     * 检出License的文件在制品包中的路径
     */
    val path: String? = null,
    /**
     * 检出License的组件
     */
    val pkgName: String? = null,
    /**
     * 检出License的组件版本
     */
    val pkgVersions: Set<String> = emptySet()
)
