package io.legado.app.ui.book.source.usedapi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.pageAccentColor
import io.legado.app.ui.theme.pageSecondaryTextColor
import io.legado.app.ui.widget.components.AppPageTopBar
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.navigationBarBottomInset
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

/**
 * 「源所用API」主界面。
 *
 * 展示一个书源用到的内置 API：
 * - 顶部 Tab 切换「已使用 / 未使用」，Tab 标签带数量统计
 * - 列表按分类分组，保留分类标题；命中的条目带对勾与强调色
 * - 顶栏开启复制模式后，点击条目复制 API 名称
 *
 * @param uiState 界面状态
 * @param onBackClick 返回按钮回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceUsedApiScreen(
    uiState: SourceUsedApiUiState,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = pageAccentColor()
    val secondaryTextColor = pageSecondaryTextColor()
    var searchKey by rememberSaveable { mutableStateOf("") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    // 复制模式：开启后点击列表条目才复制名称，图标高亮提示
    var copyMode by rememberSaveable { mutableStateOf(false) }
    val onCopyName: (String) -> Unit = { name ->
        context.sendToClip(name)
        context.toastOnUi(context.getString(R.string.api_copied, name))
    }

    AppScaffold(
        topBar = {
            AppPageTopBar(
                title = stringResource(R.string.source_used_api),
                subtitle = (uiState as? SourceUsedApiUiState.Ready)?.sourceName,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { copyMode = !copyMode }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            tint = if (copyMode) {
                                accentColor
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                    IconButton(onClick = {
                        if (searchVisible || searchKey.isNotEmpty()) {
                            searchKey = ""
                            searchVisible = false
                        } else {
                            searchVisible = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_search)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is SourceUsedApiUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SourceUsedApiUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.api_source_not_found),
                        color = secondaryTextColor
                    )
                }
            }

            is SourceUsedApiUiState.Ready -> {
                ApiCatalogContent(
                    categories = state.categories,
                    accentColor = accentColor,
                    secondaryTextColor = secondaryTextColor,
                    onCopyName = onCopyName,
                    searchKey = searchKey,
                    onSearchKeyChange = { searchKey = it },
                    showSearchField = searchVisible || searchKey.isNotEmpty(),
                    copyModeEnabled = copyMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Tab 内容区：已使用/未使用切换，下方按分类展示（分类头可折叠）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiCatalogContent(
    categories: List<ApiCategory>,
    accentColor: Color,
    secondaryTextColor: Color,
    onCopyName: (String) -> Unit,
    searchKey: String,
    onSearchKeyChange: (String) -> Unit,
    showSearchField: Boolean,
    copyModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showingUsed by rememberSaveable { mutableStateOf(true) }
    // 折叠的分类（两个 Tab 共用，切换 Tab 不会重置）
    var collapsedTypes by remember { mutableStateOf(emptySet<ApiType>()) }
    val totalCount = categories.sumOf { it.items.size }
    val usedCount = categories.sumOf { it.usedCount }
    val searchKeyTrim = searchKey.trim()

    // 当前 Tab 与搜索词下的分类（保留分类头，空分类跳过）
    val visibleCategories = categories.mapNotNull { category ->
        category.items.filter {
            it.used == showingUsed &&
                    (searchKeyTrim.isEmpty() || it.name.contains(searchKeyTrim, ignoreCase = true))
        }
            .takeIf { it.isNotEmpty() }
            ?.let { category.copy(items = it) }
    }

    Column(modifier = modifier) {
        SecondaryTabRow(selectedTabIndex = if (showingUsed) 0 else 1) {
            Tab(
                selected = showingUsed,
                onClick = { showingUsed = true },
                text = {
                    Text("${stringResource(R.string.api_used)} ($usedCount)")
                }
            )
            Tab(
                selected = !showingUsed,
                onClick = { showingUsed = false },
                text = {
                    Text("${stringResource(R.string.api_not_used)} (${totalCount - usedCount})")
                }
            )
        }

        // 搜索框固定在 Tab 下方，不随列表滚动，点击搜索图标即可见
        if (showSearchField) {
            SourceUsedApiSearchField(
                query = searchKey,
                onQueryChange = onSearchKeyChange,
                accentColor = accentColor
            )
            Spacer(Modifier.height(4.dp))
        }

        val listState = rememberLazyListState()
        // Tab 切换后回到列表顶部，避免在残留在旧列表深处的滚动位置
        LaunchedEffect(showingUsed) {
            listState.scrollToItem(0)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = navigationBarBottomInset)
        ) {
            if (visibleCategories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.api_empty_list),
                            color = secondaryTextColor
                        )
                    }
                }
            } else {
                items(visibleCategories, key = { it.type.name }) { category ->
                    val collapsed = category.type in collapsedTypes
                    CategoryHeader(
                        category = category,
                        collapsed = collapsed,
                        accentColor = accentColor,
                        secondaryTextColor = secondaryTextColor,
                        onToggle = {
                            collapsedTypes = if (collapsed) {
                                collapsedTypes - category.type
                            } else {
                                collapsedTypes + category.type
                            }
                        }
                    )
                    if (!collapsed) {
                        category.items.forEach { item ->
                            ApiItemRow(
                                item = item,
                                accentColor = accentColor,
                                secondaryTextColor = secondaryTextColor,
                                onClick = {
                                    if (copyModeEnabled) {
                                        onCopyName(item.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分类标题行（可点击折叠 / 展开）：左侧分类名，右侧条目数与展开箭头。
 */
@Composable
private fun CategoryHeader(
    category: ApiCategory,
    collapsed: Boolean,
    accentColor: Color,
    secondaryTextColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                when (category.type) {
                    ApiType.VARIABLE -> R.string.api_category_variable
                    ApiType.FUNCTION -> R.string.api_category_function
                    ApiType.SOURCE_METHOD -> R.string.api_category_source_method
                }
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = accentColor
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${category.items.size}",
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (collapsed) {
                Icons.Filled.KeyboardArrowDown
            } else {
                Icons.Filled.KeyboardArrowUp
            },
            contentDescription = null,
            tint = secondaryTextColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 单个 API 条目：命中项对勾 + 高亮，未命中置灰，点击复制名称。
 */
@Composable
private fun ApiItemRow(
    item: ApiItem,
    accentColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.used) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.used) {
                MaterialTheme.colorScheme.onSurface
            } else {
                secondaryTextColor
            }
        )
        Spacer(Modifier.weight(1f))
        if (item.used) {
            Text(
                text = stringResource(R.string.api_used),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

/**
 * API 名称搜索框，样式与书源检测页保持一致。
 */
@Composable
private fun SourceUsedApiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    accentColor: Color
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
        },
        placeholder = {
            Text(stringResource(R.string.action_search))
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            focusedLeadingIconColor = accentColor,
            cursorColor = accentColor
        )
    )
}