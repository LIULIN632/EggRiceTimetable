# 蛋炒饭课程表 — 学校索引生成器
# 用法: pwsh tools/generate_school_index.ps1
# 读取 app/src/main/assets/schools_*.json，合并输出到根目录 school_index.json
# （该文件提交到 GitHub 仓库后经 jsDelivr CDN 分发，App 内「检查更新 → 更新学校列表」拉取）
# version_id 用 TIME_YYYYMMDDHHMMSS_XXX 时间戳格式，保证字典序即时间序；同秒重复自动后缀 +1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$types = @("zhengfang", "qiangzhi", "qingguo", "chaoxing", "urp")
$schools = @{}
foreach ($t in $types) {
    $path = Join-Path $root "app\src\main\assets\schools_$t.json"
    if (Test-Path $path) {
        # 强制 UTF-8 读取（资产文件为 UTF-8 无 BOM，默认编码可能按系统 ANSI 误读）
        $schools[$t] = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
    } else {
        Write-Warning "缺少 $path，跳过该类型"
    }
}

$now = Get-Date -Format "yyyyMMddHHmmss"
$versionId = "TIME_${now}_001"
$outPath = Join-Path $root "school_index.json"
if (Test-Path $outPath) {
    $old = try { ([System.IO.File]::ReadAllText($outPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json).version_id } catch { $null }
    if ($old) {
        if ($old.StartsWith("TIME_${now}_")) {
            $n = [int]($old.Substring($old.LastIndexOf('_') + 1)) + 1
            $versionId = "TIME_${now}_" + $n.ToString("D3")
        } elseif ($old -ge $versionId) {
            Write-Warning "旧版本号 ($old) 已不早于新生成 ($versionId)，请手动调整"
        }
    }
}

$index = @{
    "protocol_version" = 1
    "version_id"       = $versionId
    "schools"          = $schools
}
# UTF-8 无 BOM 写入
[System.IO.File]::WriteAllText(
    $outPath,
    ($index | ConvertTo-Json -Depth 5),
    (New-Object System.Text.UTF8Encoding($false))
)
$count = ($schools.Values | ForEach-Object { $_.Count } | Measure-Object -Sum).Sum
Write-Host "school_index.json 已生成: $versionId（共 $count 所学校）"
