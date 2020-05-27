package com.tencent.bkrepo.repository.service.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.util.NodeUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime

/**
 * 查询条件构造工具
 *
 * @author: carrypan
 * @date: 2019-10-18
 */
object QueryHelper {

    fun nodeQuery(projectId: String, repoName: String, fullPath: String? = null, withDetail: Boolean = false): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
                .and(TNode::repoName.name).`is`(repoName)
                .and(TNode::deleted.name).`is`(null)

        val query = Query(criteria)

        fullPath?.run { criteria.and(TNode::fullPath.name).`is`(fullPath) }
        if (!withDetail) { query.fields().exclude(TNode::metadata.name) }

        return query
    }

    fun nodeListQuery(projectId: String, repoName: String, fullPathList: List<String>): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
            .and(TNode::fullPath.name).`in`(fullPathList)
        val query = Query(criteria)

        return query
    }

    fun nodeListCriteria(projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean): Criteria {
        val formattedPath = NodeUtils.formatPath(path)
        val escapedPath = NodeUtils.escapeRegex(formattedPath)
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
                .and(TNode::repoName.name).`is`(repoName)
                .and(TNode::deleted.name).`is`(null)
                .and(TNode::name.name).ne(StringPool.EMPTY)

        if (deep) {
            criteria.and(TNode::fullPath.name).regex("^$escapedPath")
        } else {
            criteria.and(TNode::path.name).`is`(formattedPath)
        }

        if (!includeFolder) { criteria.and(TNode::folder.name).`is`(false) }

        return criteria
    }

    fun nodeListQuery(projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean): Query {
        return Query.query(nodeListCriteria(projectId, repoName, path, includeFolder, deep))
            .with(Sort.by(TNode::fullPath.name))
            .apply {
                // 强制使用fullPath索引，否则mongodb会使用path索引，不能达到最优索引
                if (deep) {
                    this.withHint(TNode.FULL_PATH_IDX_DEF)
                }
            }
    }

    fun nodePageQuery(projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean, page: Int, size: Int): Query {
        return nodeListQuery(projectId, repoName, path, includeFolder, deep)
            .with(PageRequest.of(page, size))
    }

    fun nodePathUpdate(path: String, name: String, operator: String): Update {
        return update(operator)
                .set(TNode::path.name, path)
                .set(TNode::name.name, name)
                .set(TNode::fullPath.name, path + name)
    }

    fun nodeExpireDateUpdate(expireDate: LocalDateTime?, operator: String): Update {
        return update(operator).apply {
            expireDate?.let { set(TNode::expireDate.name, expireDate) } ?: run { unset(TNode::expireDate.name) }
        }
    }

    fun nodeDeleteUpdate(operator: String): Update {
        return update(operator)
            .set(TNode::deleted.name, LocalDateTime.now())
    }

    private fun update(operator: String): Update {
        return Update()
                .set(TNode::lastModifiedDate.name, LocalDateTime.now())
                .set(TNode::lastModifiedBy.name, operator)
    }
}
