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

package com.tencent.bkrepo.lfs.service

import com.tencent.bkrepo.auth.api.ServiceTemporaryTokenClient
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.auth.pojo.token.TokenType
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_UID
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.lfs.artifact.LfsArtifactInfo
import com.tencent.bkrepo.lfs.artifact.LfsProperties
import com.tencent.bkrepo.lfs.constant.BASIC_TRANSFER
import com.tencent.bkrepo.lfs.constant.UPLOAD_OPERATION
import com.tencent.bkrepo.lfs.pojo.ActionDetail
import com.tencent.bkrepo.lfs.pojo.BatchRequest
import com.tencent.bkrepo.lfs.pojo.BatchResponse
import com.tencent.bkrepo.lfs.pojo.LfsObject
import com.tencent.bkrepo.lfs.utils.OidUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.stereotype.Service

@Service
class ObjectService(
    private val nodeClient: NodeClient,
    private val temporaryTokenClient: ServiceTemporaryTokenClient,
    private val repositoryClient: RepositoryClient,
    private val permissionManager: PermissionManager,
    private val lfsProperties: LfsProperties
) : ArtifactService() {

    /**
     * [batch api实现](https://github.com/git-lfs/git-lfs/blob/main/docs/api/batch.md)
     */
    fun batch(projectId: String, repoName: String, request: BatchRequest): BatchResponse {
        Preconditions.checkArgument(request.transfers.contains(BASIC_TRANSFER), BatchRequest::transfers.name)
        val permissionAction = if (request.operation == UPLOAD_OPERATION) {
            PermissionAction.WRITE
        } else {
            PermissionAction.READ
        }
        permissionManager.checkRepoPermission(permissionAction, projectId, repoName)
        val repo = repositoryClient.getRepoDetail(projectId, repoName).data!!
        if (request.operation == UPLOAD_OPERATION && !repoAllowUpload(repo)) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, BatchRequest::operation)
        }
        val objects = buildLfsObjects(request, repo)
        return BatchResponse(BASIC_TRANSFER, objects)
    }

    fun upload(lfsArtifactInfo: LfsArtifactInfo, file: ArtifactFile) {
        with(lfsArtifactInfo) {
            nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data ?: return
        }
        val context = ArtifactUploadContext(file)
        repository.upload(context)
    }

    fun download(lfsArtifactInfo: LfsArtifactInfo) {
        val context = ArtifactDownloadContext()
        repository.download(context)
    }

    private fun createTemporaryToken(
        projectId: String,
        repoName: String,
        tokenType: TokenType,
        userId: String
    ) = temporaryTokenClient.createToken(
        TemporaryTokenCreateRequest(
            projectId = projectId,
            repoName = repoName,
            fullPathSet = setOf(StringPool.ROOT),
            type = tokenType,
            expireSeconds = TOKEN_EXPIRES_SECONDS,
            authorizedUserSet = setOf(userId)
        )
    ).data!!.first().token

    private fun buildLfsObjects(
        request: BatchRequest,
        repo: RepositoryDetail
    ): List<LfsObject> {
        val userId = SecurityUtils.getUserId()
        val tokenType = if (request.operation == UPLOAD_OPERATION) {
            TokenType.UPLOAD
        } else {
            TokenType.DOWNLOAD
        }
        val token = createTemporaryToken(repo.projectId, repo.name, tokenType, userId)
        return request.objects.map {
            if (request.operation == UPLOAD_OPERATION && checkObjectExist(repo, it.oid)) {
                it
            } else {
                it.copy(
                    authenticated = true,
                    actions = mapOf(
                        request.operation to ActionDetail(
                            href = lfsProperties.domain.removeSuffix(StringPool.SLASH) +
                                "/lfs/${repo.projectId}/${repo.name}/${it.oid}" +
                                "?size=${it.size}&ref=${request.ref["name"]}",
                            header = mapOf(
                                HttpHeaders.AUTHORIZATION to "Temporary $token",
                                AUTH_HEADER_UID to userId
                            ),
                            expiresIn = TOKEN_EXPIRES_SECONDS
                        )
                    )
                )
            }
        }
    }

    private fun checkObjectExist(repo: RepositoryDetail, oid: String): Boolean {
        val realPath = OidUtils.convertToFullPath(oid)
        val node = nodeClient.getNodeDetail(repo.projectId, repo.name, realPath).data
        return node != null
    }

    private fun repoAllowUpload(repo: RepositoryDetail): Boolean {
        return repo.category == RepositoryCategory.LOCAL || repo.category == RepositoryCategory.COMPOSITE
    }

    companion object {
        private const val TOKEN_EXPIRES_SECONDS = 86400L
    }
}
