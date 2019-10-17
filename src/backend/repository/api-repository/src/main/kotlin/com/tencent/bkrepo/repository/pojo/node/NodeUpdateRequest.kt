package com.tencent.bkrepo.repository.pojo.node

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 更新节点请求
 *
 * @author: carrypan
 * @date: 2019-09-22
 */
@ApiModel("更新节点请求")
data class NodeUpdateRequest(
    @ApiModelProperty("所属项目", required = true)
    val projectId: String,
    @ApiModelProperty("仓库名称", required = true)
    val repoName: String,
    @ApiModelProperty("节点完整路径", required = true)
    val fullPath: String,
    @ApiModelProperty("节点新的完整路径")
    val newFullPath: String? = null,
    @ApiModelProperty("过期时间，单位天(0代表永久保存)")
    val expires: Long? = null,

    @ApiModelProperty("操作用户")
    val operator: String
)
