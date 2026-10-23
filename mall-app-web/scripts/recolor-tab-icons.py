# -*- coding: utf-8 -*-
""" tabBar 图标重上色脚本

将 6 枚 tabBar 图标整体重上色为摊位主题双色：
- 默认态 -> 灰 #8A7A6D（与 pages.json tabBar.color 一致）
- 选中态 -> 红 #C23A22（与 pages.json tabBar.selectedColor 一致）

保留原图形的形状与抗锯齿（仅替换 RGB，保留 alpha 通道）。
用法：python scripts/recolor-tab-icons.py
"""
import os

from PIL import Image

STATIC_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', 'src', 'static'))

GRAY = (138, 122, 109)  # #8A7A6D
RED = (194, 58, 34)     # #C23A22

# 文件名 -> 目标色
JOBS = {
    'tab-home.png': GRAY,
    'tab-cate.png': GRAY,
    'tab-my.png': GRAY,
    'tab-home-current.png': RED,
    'tab-cate-current.png': RED,
    'tab-my-current.png': RED,
}


def recolor(path: str, color: tuple) -> None:
    im = Image.open(path).convert('RGBA')
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            # 透明像素保持全透明；可见/半透明像素统一为目标色，alpha 原样保留
            px[x, y] = (*color, a) if a else (0, 0, 0, 0)
    im.save(path)
    print(f'recolored: {os.path.basename(path)} -> #{color[0]:02X}{color[1]:02X}{color[2]:02X}')


if __name__ == '__main__':
    for name, color in JOBS.items():
        recolor(os.path.join(STATIC_DIR, name), color)
