package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest

/**
 * 节点服务接口
 */
interface NodeService {
    /**
     * 查询节点详情
     */
    fun detail(projectId: String, repoName: String, fullPath: String, repoType: String? = null): NodeDetail?

    /**
     * 计算文件或者文件夹大小
     */
    fun computeSize(projectId: String, repoName: String, fullPath: String): NodeSizeInfo

    /**
     * 查询文件节点数量
     */
    fun countFileNode(projectId: String, repoName: String, path: String = ROOT): Long

    /**
     * 列表查询节点
     */
    fun list(
        projectId: String,
        repoName: String,
        path: String,
        includeFolder: Boolean = true,
        includeMetadata: Boolean = false,
        deep: Boolean = false
    ): List<NodeInfo>

    /**
     * 分页查询节点
     */
    fun page(
        projectId: String,
        repoName: String,
        path: String,
        pageNumber: Int,
        pageSize: Int,
        includeFolder: Boolean = true,
        includeMetadata: Boolean = false,
        deep: Boolean = false,
        sort: Boolean = false
    ): Page<NodeInfo>

    /**
     * 判断节点是否存在
     */
    fun exist(projectId: String, repoName: String, fullPath: String): Boolean

    /**
     * 判断节点列表是否存在
     */
    fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String>

    /**
     * 创建节点，返回节点详情
     */
    fun create(createRequest: NodeCreateRequest): NodeDetail

    /**
     * 重命名文件或者文件夹
     * 重命名过程中出现错误则抛异常，剩下的文件不会再移动
     * 遇到同名文件或者文件夹直接抛异常
     */
    fun rename(renameRequest: NodeRenameRequest)

    /**
     * 更新节点
     */
    fun update(updateRequest: NodeUpdateRequest)

    /**
     * 移动文件或者文件夹
     * 采用fast-failed模式，移动过程中出现错误则抛异常，剩下的文件不会再移动
     * 行为类似linux mv命令
     * mv 文件名 文件名	将源文件名改为目标文件名
     * mv 文件名 目录名	将文件移动到目标目录
     * mv 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）移动到目标目录；目标目录不存在则改名
     * mv 目录名 文件名	出错
     */
    fun move(moveRequest: NodeMoveRequest)

    /**
     * 拷贝文件或者文件夹
     * 采用fast-failed模式，拷贝过程中出现错误则抛异常，剩下的文件不会再拷贝
     * 行为类似linux cp命令
     * cp 文件名 文件名	将源文件拷贝到目标文件
     * cp 文件名 目录名	将文件移动到目标目录下
     * cp 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）拷贝到目标目录；目标目录不存在则将源目录下文件拷贝到目标目录
     * cp 目录名 文件名	出错
     */
    fun copy(copyRequest: NodeCopyRequest)

    /**
     * 删除指定节点, 逻辑删除
     */
    fun delete(deleteRequest: NodeDeleteRequest)

    /**
     * 根据全路径删除文件或者目录
     */
    fun deleteByPath(projectId: String, repoName: String, fullPath: String, operator: String)
}
