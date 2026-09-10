# CI/CD 配置参考（GitHub Actions）

> 本文档记录 `.github/workflows/` 下各 workflow 的职责与触发条件，属**工程配置参考**。
> **什么时候读**：CI 报错、要调整 workflow、想确认发版 / 构建流程时。
> 注意：CI 中 lint 通过视为"完成"的一部分（`./gradlew lint` 本地也要跑）。

## 当前 workflows

| 文件                   | 名称                   | 触发                                                   | 作用                                                                                       |
| ---------------------- | ---------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| `test.yml`             | Test Build             | push / pull_request / workflow_run / workflow_dispatch | 构建全部 3 个 release flavor；自动创建 GitHub/Gitee 发布并从 `updateLog.md` 生成 changelog |
| `web.yml`              | Build Web              | push / pull_request / workflow_dispatch                | `modules/web/` 有过改动时构建 Vue 前端，并把产物提交到 `app/src/main/assets/web/vue/`      |
| `cronet.yml`           | Update Cronet          | schedule / workflow_dispatch                           | 更新 Cronet 原生库                                                                         |
| `lint.yaml`            | Quick Lint and Compile | push / pull_request                                    | 跑 lint + 编译；lint 通过视为 CI 完成的一部分                                              |
| `unit-test.yaml`       | Unit Test              | push / pull_request                                    | 单元测试                                                                                   |
| `build-onlyDebug.yaml` | Build Debug APK        | push / pull_request                                    | 构建 Debug APK                                                                             |
| `release.yml`          | Release Build 双包打包 | workflow_dispatch                                      | 手动触发，打双包发布                                                                       |
| `stale.yml`            | closeStaleIssue        | schedule / workflow_dispatch                           | 清理 stale issue                                                                           |

## 2. 使用注意

- workflow 文件以推送分支为准，本地改动后推到对应分支才会触发。
- 涉及签名密钥的关键文件在 workflows 目录内，发布相关改动要核对 `release.yml` 与密钥引用。
- 新加 workflow 后在本表登记，保持索引与实际文件一致（与 project-rules 维护约定一致）。
