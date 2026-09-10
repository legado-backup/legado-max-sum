# claude-superpowers（已归档技能集）

本目录是原先挂在仓库根 `.claude/skills/` 的 superpowers 体系技能包，于 2026-09 归档至此。

## 来源与背景

- 这批技能来自 [obra/superpowers](https://github.com/obra/superpowers) 一键安装产物（含 brainstorming、writing-plans、executing-plans、subagent-driven-development、using-git-worktrees 等），以及少量当时为本项目定制的 `legado-*` 技能（如 Android 崩溃调试、中文文档/Git 规范等）。
- 仓库当时的配套决策（`docs/superpowers/`、`docs/archive/superpowers/`）中的计划与规格文档仍保留在原处，未随本次迁移处理；如不再需要请另行评估归档。

## 归档原因

- 此后可调用技能的唯一来源是**用户级技能注册表**（`.agents/skills`），仓库内的技能包不再被加载，保留在原位置会与原 CLAUDE.md 的技能引用产生死链接。
- superpowers 的通用流程技能已由注册表中的现代技能覆盖（`code-review`、`grill-with-docs`、`to-spec`、`to-tickets`、`research`、`tdd` 等）。

## 迁移信息

- 迁移时间：2026-09-10
- 迁移路径：`.claude/skills/**` → `docs/archive/skills/claude-superpowers/`（git mv，历史可回溯）。
- 需要取出某个技能时：`git mv docs/archive/skills/claude-superpowers/<技能名> <目的路径>` 即可，如需回归使用请先评估其与当前 `.agents/skills` 是否重复。