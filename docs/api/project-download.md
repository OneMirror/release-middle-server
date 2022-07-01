### 上传文件
`PUT` `https://m.onemirror.net/$projectName/$versionName/$buildNumber/$fileName`
- Headers \
`Change-List` 修改列表，一般为 git commit message \
`Checksum` (可选) 文件的 sha256, 如果失败，则返回400
- Return status \
Success: 204 No Content \
Wrong content: 400 Bad Request \
Server error: 500 Internal Server Error

## 更新文件
`POST` `https://m.onemirror.net/$projectName/$versionName/$buildNumber/$fileName`
- Content `json` \
`status` (可选) 状态，enum (SYNCING, SUCCESS, FAILED) \
`commitMessage` (可选) 提交信息 \
`hash` (可选) 文件的hash
- Return status \
204 No Content