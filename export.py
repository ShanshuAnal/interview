import os

# 配置需要提取的文件后缀和需要跳过的无关目录
ALLOWED_EXTENSIONS = {'.java', '.xml', '.yml', '.yaml', '.properties', '.sql', '.md'}
IGNORED_DIRS = {'.git', '.idea', 'target', 'build', '.vscode', 'logs', 'node_modules'}

output_filename = 'springboot_code.txt'

with open(output_filename, 'w', encoding='utf-8') as outfile:
    for root, dirs, files in os.walk('.'):
        # 动态修改 dirs 列表，跳过不需要扫描的目录
        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS]

        for file in files:
            if any(file.endswith(ext) for ext in ALLOWED_EXTENSIONS):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as infile:
                        outfile.write(f"========== 文件: {filepath} ==========\n")
                        outfile.write(infile.read() + "\n\n")
                except Exception as e:
                    print(f"跳过无法读取的文件: {filepath}")

print(f"代码提取完成！请查看当前目录下的 {output_filename}")