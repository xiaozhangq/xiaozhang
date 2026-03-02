const fs = require('fs')
const path = require('path')

const distDir = path.join(__dirname, '../dist')
const staticDir = path.join(__dirname, '../../backend/src/main/resources/static')

if (!fs.existsSync(distDir)) {
  console.error('请先执行 npm run build')
  process.exit(1)
}

if (!fs.existsSync(staticDir)) {
  fs.mkdirSync(staticDir, { recursive: true })
}

function copyRecursive(src, dest) {
  const stat = fs.statSync(src)
  if (stat.isDirectory()) {
    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true })
    for (const name of fs.readdirSync(src)) {
      copyRecursive(path.join(src, name), path.join(dest, name))
    }
  } else {
    fs.copyFileSync(src, dest)
  }
}

// 清空 static 后复制（保留 .gitkeep 则只删内容）
const entries = fs.readdirSync(staticDir)
for (const e of entries) {
  if (e === '.gitkeep') continue
  const p = path.join(staticDir, e)
  if (fs.statSync(p).isDirectory()) fs.rmSync(p, { recursive: true })
  else fs.unlinkSync(p)
}

copyRecursive(distDir, staticDir)
console.log('已复制 frontend/dist 到 backend/src/main/resources/static')
// 保留 .gitkeep
const gitkeep = path.join(staticDir, '.gitkeep')
if (!fs.existsSync(gitkeep)) fs.writeFileSync(gitkeep, '')
