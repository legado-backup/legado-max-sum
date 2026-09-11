package io.legado.app.ui.book.source.usedapi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 展示一个书源用到的全部内置 API 目录，并按分类展示：
 * 命中的条目以对勾与强调色高亮并排前，未使用的置灰，
 * 点击条目复制 API 名称。
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
    val onCopyName: (String) -> Unit = { name ->
        context.sendToClip(name)
        context.toastOnUi(context.getString(R.string.api_copied, name))
    }

    AppScaffold(
        topBar = {
            AppPageTopBar(
                title = stringResource(R.string.source_used_api),
                subtitle = (uiState as? SourceUsedApiUiState.Ready)?.sourceName,
                onBackClick = onBackClick
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = navigationBarBottomInset)
                ) {
                    items(state.categories, key = { it.type.name }) { category ->
                        CategoryHeader(category, accentColor, secondaryTextColor)
                        category.items.forEach { item ->
                            ApiItemRow(
                                item = item,
                                accentColor = accentColor,
                                secondaryTextColor = secondaryTextColor,
                                onClick = { onCopyName(item.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分类标题行：左侧分类名，右侧命中数（如 3/138）。
 */
@Composable
private fun CategoryHeader(
    category: ApiCategory,
    accentColor: androidx.compose.ui.graphics.Color,
    secondaryTextColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            text = "${category.usedCount}/${category.items.size}",
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
        )
    }
}

/**
 * 单个 API 条目：命中项对勾 + 高亮，未命中置灰，点击复制名称。
 */
@Composable
private fun ApiItemRow(
    item: ApiItem,
    accentColor: androidx.compose.ui.graphics.Color,
    secondaryTextColor: androidx.compose.ui.graphics.Color,
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