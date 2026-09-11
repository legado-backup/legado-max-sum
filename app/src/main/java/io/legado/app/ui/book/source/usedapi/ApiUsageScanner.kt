package io.legado.app.ui.book.source.usedapi

/**
 * 内置 API 命中分类。
 */
enum class ApiType {
    /** 内置绑定变量（如 `cookie.get(...)`、`result`） */
    VARIABLE,

    /** 内置函数，`xxx()` 或 `java.xxx()` 调用 */
    FUNCTION,

    /** 源对象方法，`source.xxx()` 调用 */
    SOURCE_METHOD
}

/**
 * 单个内置 API 条目。
 *
 * @param name 目录名称（规则中的调用名）
 * @param used 该书源是否用到了该 API
 */
data class ApiItem(val name: String, val used: Boolean)

/**
 * 一组内置 API 及命中情况，命中的条目排在前。
 *
 * @param type 分类
 * @param items 该分类下的全部条目
 */
data class ApiCategory(
    val type: ApiType,
    val items: List<ApiItem>
) {
    /** 该分类下命中的数量 */
    val usedCount: Int
        get() = items.count { it.used }
}

/**
 * 静态扫描书源规则文本，判定其用到了 [BuiltInApiCatalog] 中的哪些内置 API。
 *
 * 输入为 `Gson.toJson(bookSource)` 序列化后的完整规则 JSON（含 searchUrl、jsLib、
 * loginUrl、全部 rule 子对象及规则里的 `@js:`、`<js>`、`{{...}}` 片段），
 * 一次覆盖源的全部 JS 代码。
 */
object ApiUsageScanner {

    /**
     * 扫描书源规则文本，返回三个分类的完整目录与命中标记。
     *
     * 匹配示例：
     * - 变量命中：`cookie.get(...)`、`result`（变量名后需紧跟 `.`/`[`/`(`/运算符等续符）
     * - 函数命中：`ajax(url)` 与 `java.ajax(url)` 两种调用形式都命中
     * - 源对象方法命中：`source.getVariable("x")`
     *
     * 注：纯文本扫描对规则字段名/CSS 选择器中的同名词存在少量误报，
     * 靠「变量名后随续符」「函数名后随 `(`」「`source` 前缀」收窄；
     * 字段名如 `bookSourceUrl`、`chapterName` 因其后随词字符不会误报。
     */
    fun scan(ruleJsonText: String): List<ApiCategory> = listOf(
        ApiCategory(ApiType.VARIABLE, matchItems(BuiltInApiCatalog.bindingVariables, ruleJsonText) {
            isVariableUsed(it, ruleJsonText)
        }),
        ApiCategory(ApiType.FUNCTION, matchItems(BuiltInApiCatalog.javaMethods, ruleJsonText) {
            isFunctionUsed(it, ruleJsonText)
        }),
        ApiCategory(ApiType.SOURCE_METHOD, matchItems(BuiltInApiCatalog.sourceMethods, ruleJsonText) {
            isSourceMethodUsed(it, ruleJsonText)
        })
    )

    private fun matchItems(
        names: List<String>,
        text: String,
        matcher: (String) -> Boolean
    ): List<ApiItem> {
        return names
            .map { ApiItem(it, matcher(it)) }
            .sortedByDescending { it.used }
    }

    /**
     * 绑定变量命中：变量名前不能是词字符/`.`/`$`，且后随 `.`、`[`、`(`、空白或常见 JS 运算符。
     */
    private fun isVariableUsed(name: String, text: String): Boolean {
        val regex = Regex("(?<![.\\w$])${Regex.escape(name)}(?=[.\\[(\\s=+\\-*/%&|!,?;<>}\\])])")
        return regex.containsMatchIn(text)
    }

    /**
     * 函数命中：方法名前不能是词字符/`$`，后随 `(`（`java.ajax(` 中 `.` 前亦命中）。
     */
    private fun isFunctionUsed(name: String, text: String): Boolean {
        val regex = Regex("(?<![\\w$])${Regex.escape(name)}\\s*\\(")
        return regex.containsMatchIn(text)
    }

    /**
     * 源对象方法：`source` 后随 `.` 与方法名与 `(`。
     */
    private fun isSourceMethodUsed(name: String, text: String): Boolean {
        val regex = Regex("(?<![\\w$])source\\s*\\.\\s*${Regex.escape(name)}\\s*\\(")
        return regex.containsMatchIn(text)
    }
}