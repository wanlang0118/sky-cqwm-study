# 音频资源说明

将提示音文件放在本目录下，例如：
- `preview.mp3` （来单提醒/付款成功提示）
- `reminder.mp3` （客户催单提示）

前端可通过静态路径引用：
- `http://<host>:<port>/audio/preview.mp3`
- `http://<host>:<port>/audio/reminder.mp3`

也可以按需替换文件名并在前端代码中对应调整。
