package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.pojo.IdValue
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.constant.SERVICE_NAME
import com.tencent.bkrepo.repository.pojo.Node
import com.tencent.bkrepo.repository.pojo.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.NodeUpdateRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 资源节点服务接口
 *
 * @author: carrypan
 * @date: 2019-09-10
 */
@Api("节点服务接口")
@FeignClient(SERVICE_NAME, contextId = "NodeResource")
@RequestMapping("/service/resource")
interface NodeResource {

    @ApiOperation("查看节点详情")
    @GetMapping("/{id}")
    fun detail(
        @ApiParam(value = "节点id")
        @PathVariable id: String
    ): Response<Node>

    @ApiOperation("列表查询指定目录下所有节点, 只返回一层深度的节点")
    @GetMapping("/list/{repositoryId}")
    fun list(
        @ApiParam(value = "仓库id")
        @PathVariable repositoryId: String,
        @ApiParam(value = "所属目录")
        @RequestParam path: String
    ): Response<List<Node>>

    @ApiOperation("分页查询指定目录下所有节点, 只返回一层深度的节点")
    @GetMapping("/page/{page}/{size}/{repositoryId}")
    fun page(
        @ApiParam(value = "当前页")
        @PathVariable page: Int,
        @ApiParam(value = "分页大小")
        @PathVariable size: Int,
        @ApiParam(value = "仓库id")
        @PathVariable repositoryId: String,
        @ApiParam(value = "所属目录")
        @RequestParam path: String
    ): Response<Page<Node>>

    @ApiOperation("创建节点")
    @PostMapping
    fun create(
        @ApiParam(value = "创建节点请求")
        @RequestBody nodeCreateRequest: NodeCreateRequest
    ): Response<IdValue>

    @ApiOperation("修改节点")
    @PutMapping("/{id}")
    fun update(
        @ApiParam(value = "节点id")
        @PathVariable id: String,
        @ApiParam(value = "更新节点请求")
        @RequestBody nodeUpdateRequest: NodeUpdateRequest
    ): Response<Void>

    @ApiOperation("根据id删除节点")
    @DeleteMapping("/{id}")
    fun deleteById(
        @ApiParam(value = "节点id")
        @PathVariable id: String
    ): Response<Void>

    @ApiOperation("删除目录")
    @DeleteMapping
    fun deleteByPath(
        @ApiParam(value = "仓库id")
        @RequestParam repositoryId: String,
        @ApiParam(value = "节点目录")
        @RequestParam path: String
    ): Response<Void>

}
