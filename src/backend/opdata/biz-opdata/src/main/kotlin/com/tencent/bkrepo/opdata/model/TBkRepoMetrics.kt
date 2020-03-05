package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.opdata.pojo.RepoMetrics
import org.springframework.data.mongodb.core.mapping.Document

@Document("bkrepo_metrics")
data class TBkRepoMetrics(
    var date:String,
    var projectNum: Long,
    var nodeNum: Long,
    var capSize: Long
)
