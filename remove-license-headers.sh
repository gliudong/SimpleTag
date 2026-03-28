#!/bin/bash
# 批量删除GPL许可证头注释

# 查找所有包含许可证头的 .kt 和 .kts 文件
find /Users/liudong/Documents/GitHub/SimpleTag/app/src/main/java -type f \( -name "*.kt" -o -name "*.kts" \) | while read -r file; do
    # 使用 sed 删除许可证头块
    # 匹配从 "/*" 开始到 "*/" 结束的多行注释
    sed -i '' '/^\/\*$/,/^ \*\/$/d' "$file"
    echo "Processed: $file"
done

echo "Done! License headers removed."
