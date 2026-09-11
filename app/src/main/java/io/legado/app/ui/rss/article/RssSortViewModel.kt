package io.legado.app.ui.rss.article

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.help.source.removeSortCache
import kotlinx.coroutines.launch

/**
 * RSS 分类/排序页 ViewModel
 *
 * 依赖经 [RssSortRepository] 构造注入（默认 Default 实现），
 * JVM 单测可直接 Fake（testing.md §16）
 */
class RssSortViewModel(
    private val repository: RssSortRepository = RssSortRepository.Default
) : ViewModel() {

    var url: String? = null
    var sortUrl: String? = null
    var rssSource: RssSource? = null
    var order = System.currentTimeMillis()
    val articleStyle get() = rssSource?.articleStyle
    var searchKey: String? = null
    var sourceName: String? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        viewModelScope.launch {
            try {
                url = intent.getStringExtra("sourceUrl")
                url?.let { key ->
                    rssSource = repository.getSourceByKey(key)
                    rssSource?.let {
                        sourceName = it.sourceName
                    } ?: run {
                        rssSource = RssSource(sourceUrl = key)
                    }
                }
                sortUrl = intent.getStringExtra("sortUrl") ?: sortUrl
                searchKey = intent.getStringExtra("key")
            } finally {
                onFinally()
            }
        }
    }

    fun switchLayout() {
        val source = rssSource ?: return
        if (source.articleStyle < 4) {
            source.articleStyle += 1
        } else {
            source.articleStyle = 0
        }
        viewModelScope.launch {
            repository.updateSource(source)
        }
    }

    fun clearArticles() {
        viewModelScope.launch {
            url?.let { repository.deleteArticles(it) }
            order = System.currentTimeMillis()
        }
    }

    fun clearSortCache(onFinally: () -> Unit) {
        viewModelScope.launch {
            try {
                rssSource?.removeSortCache()
            } finally {
                onFinally()
            }
        }
    }

    fun getRecords(origin: String? = null): List<RssReadRecord> = repository.getRecords(origin)

    fun countRecords(origin: String? = null): Int = repository.countRecords(origin)

    fun deleteAllRecord(origin: String? = null) {
        viewModelScope.launch {
            repository.deleteRecords(origin)
        }
    }
}