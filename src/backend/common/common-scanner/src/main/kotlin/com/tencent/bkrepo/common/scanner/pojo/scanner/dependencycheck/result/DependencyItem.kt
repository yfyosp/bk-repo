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

package com.tencent.bkrepo.common.scanner.pojo.scanner.dependencycheck.result

import com.fasterxml.jackson.annotation.JsonAlias
import com.tencent.bkrepo.common.checker.pojo.Cvssv2
import com.tencent.bkrepo.common.checker.pojo.Cvssv3
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DependencyCheck漏洞信息")
data class DependencyItem(
    /**
     * CVE-2017-18349
     */
    @ApiModelProperty("cve id")
    val cveId: String,

    /**
     * CVE-2017-18349
     */
    @ApiModelProperty("漏洞名/漏洞标题")
    val name: String,

    @ApiModelProperty("所属依赖")
    val dependency: String,

    @ApiModelProperty("引入版本")
    val version: String,

    /**
     * CRITICAL,HIGH,MEDIUM,LOW
     */
    @ApiModelProperty("等级")
    val severity: String,

    @ApiModelProperty("漏洞描述")
    @JsonAlias("description")
    val description: String,

    @ApiModelProperty("官方解决方案")
    @JsonAlias("official_solution")
    val officialSolution: String?,

    @ApiModelProperty("解决方案")
    @JsonAlias("defense_solution")
    val defenseSolution: String?,

    @ApiModelProperty("相关链接")
    val references: List<String>?,

    @ApiModelProperty("cvss V2 漏洞影响评价")
    val cvssV2Vector: Cvssv2?,

    @ApiModelProperty("cvss V3 漏洞影响评价")
    val cvssV3: Cvssv3?,

    @ApiModelProperty("漏洞文件路径")
    val path: String? = null
)
