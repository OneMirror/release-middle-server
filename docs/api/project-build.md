## 更新构建
`POST` `https://m.onemirror.net/$projectName/$versionName/$buildNumber/`
- Content `json` \
`status` (可选) 状态，enum (SYNCING, SUCCESS, FAILED) \
`commitMessage` (可选) 提交信息
- Response \
`BuildNumber` 构建号
- Other \
在 `buildNumber` 为 `0` 时，会分配最新的可用 `buildNumber`
