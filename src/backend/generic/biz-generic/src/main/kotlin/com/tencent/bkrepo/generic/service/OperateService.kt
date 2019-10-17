package com.tencent.bkrepo.generic.service

import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.CommonMessageCode
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.ExternalErrorCodeException
import com.tencent.bkrepo.generic.pojo.FileDetail
import com.tencent.bkrepo.generic.pojo.FileInfo
import com.tencent.bkrepo.generic.pojo.FileSizeInfo
import com.tencent.bkrepo.generic.pojo.operate.FileSearchRequest
import com.tencent.bkrepo.repository.api.NodeResource
import com.tencent.bkrepo.repository.pojo.node.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSearchRequest
import com.tencent.bkrepo.repository.pojo.node.NodeUpdateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * 文件操作服务类
 *
 * @author: carrypan
 * @date: 2019-10-13
 */
@Service
class OperateService(
    private val nodeResource: NodeResource,
    private val authService: AuthService
) {
    fun listFile(userId: String, projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean): List<FileInfo> {
        authService.checkPermission(CheckPermissionRequest(userId, ResourceType.REPO, PermissionAction.READ, projectId, repoName))
        return nodeResource.list(projectId, repoName, path, includeFolder, deep).data?.map { toFileInfo(it) } ?: emptyList()
    }

    fun searchFile(userId: String, searchRequest: FileSearchRequest): List<FileInfo> {
        authService.checkPermission(CheckPermissionRequest(userId, ResourceType.REPO, PermissionAction.READ, searchRequest.projectId, searchRequest.repoName))
        return nodeResource.search(
                searchRequest.let {
                    NodeSearchRequest(
                            projectId = it.projectId,
                            repoName = it.repoName,
                            pathPattern = it.pathPattern,
                            metadataCondition = it.metadataCondition
                    )
                }
        ).data?.map { toFileInfo(it) } ?: emptyList()
    }

    fun getFileDetail(userId: String, projectId: String, repoName: String, fullPath: String): FileDetail {
        authService.checkPermission(CheckPermissionRequest(userId, ResourceType.REPO, PermissionAction.READ, projectId, repoName))
        val nodeDetail = nodeResource.queryDetail(projectId, repoName, fullPath).data ?: throw ErrorCodeException(CommonMessageCode.ELEMENT_NOT_FOUND, fullPath)
        return FileDetail(toFileInfo(nodeDetail.nodeInfo), nodeDetail.metadata)
    }

    fun getFileSize(userId: String, projectId: String, repoName: String, fullPath: String): FileSizeInfo {
        authService.checkPermission(CheckPermissionRequest(userId, ResourceType.REPO, PermissionAction.READ, projectId, repoName))
        val nodeSizeInfo = nodeResource.getSize(projectId, repoName, fullPath).data ?: throw ErrorCodeException(CommonMessageCode.ELEMENT_NOT_FOUND, fullPath)
        return FileSizeInfo(subFileCount = nodeSizeInfo.subNodeCount, size = nodeSizeInfo.size)
    }

    fun mkdir(userId: String, projectId: String, repoName: String, fullPath: String) {
        authService.checkPermission(CheckPermissionRequest(userId, ResourceType.REPO, PermissionAction.WRITE, projectId, repoName))
        val fullUrl = "$projectId/$repoName/$fullPath"
        val createRequest = NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = true,
                fullPath = fullPath,
                overwrite = false,
                operator = userId
        )
        val result = nodeResource.create(createRequest)

        if (result.isNotOk()) {
            logger.warn("user[$userId] mkdirs [$fullUrl] failed: [${result.code}, ${result.message}]")
            throw ExternalErrorCodeException(result.code, result.message)
        }

        logger.info("user[$userId] mkdirs [$fullUrl] success")
    }

    fun move(userId: String, projectId: String, repoName: String, fullPath: String, toPath: String) {
        // TODO: 鉴权

        nodeResource.queryDetail(projectId, repoName, fullPath).data ?: throw ErrorCodeException(CommonMessageCode.ELEMENT_NOT_FOUND, fullPath)

        val updateRequest = NodeUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                newFullPath = toPath,
                operator = userId
        )
        val result = nodeResource.update(updateRequest)

        if (result.isNotOk()) {
            logger.warn("user[$userId] move [$projectId/${repoName}$fullPath] to [$toPath] failed: [${result.code}, ${result.message}]")
            throw ExternalErrorCodeException(result.code, result.message)
        }
    }

    fun copy(userId: String, projectId: String, repoName: String, fullPath: String, toProjectId: String, toRepoName: String, toPath: String) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OperateService::class.java)

        fun toFileInfo(nodeInfo: NodeInfo): FileInfo {
            return nodeInfo.let { FileInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    folder = it.folder,
                    path = it.path,
                    name = it.name,
                    fullPath = it.fullPath,
                    size = it.size,
                    sha256 = it.sha256
            ) }
        }
    }
}
