#!/usr/bin/env python3
"""Refresh the gallery / demo landing pages after a product-catalog swap.

- gallery thumbnails point at preview/ (current interactive build)
- both landing pages get a 16-product catalog grid (single-language Chinese)
- both landing pages get a three-language quick-launch strip
- copy mentions the real products and the en / fr / zh single-language modes

Idempotent: catalog and language blocks are marker-delimited.
Usage: python3 tools/update_entry_pages.py
"""
import json, os, re, shutil

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = json.load(open(os.path.join(HERE, 'products.json')))['products']
I18N = json.load(open(os.path.join(HERE, 'i18n.json')))
PROD_I18N = I18N['products']
CAT_ZH = {
    'CLASSICS': I18N['t']['CLASSICS'][2],
    'ESPRESSO': I18N['t']['ESPRESSO'][2],
    'MILK COFFEE': I18N['t']['MILK COFFEE'][2],
    'ICED': I18N['t']['ICED'][2],
    'OTHER': I18N['t']['OTHER'][2],
}

CSS = """
.catalog{display:grid;grid-template-columns:repeat(auto-fill,minmax(146px,1fr));gap:14px;margin:18px 0 8px}
.pitem{background:#fff;border:1px solid #E6DDCF;border-radius:12px;overflow:hidden;text-align:center;padding-bottom:10px}
.pitem img{width:100%;aspect-ratio:1;object-fit:cover;display:block;background:#F7F3EC}
.pitem b{display:block;font-size:13px;margin-top:8px;padding:0 6px;line-height:1.35}
.pitem span{display:block;font-size:11px;color:#8C7A66;padding:0 6px;margin-top:3px}
.catnote{font-size:13px;color:#7C6650;line-height:1.7}
.langlaunch{display:flex;flex-wrap:wrap;gap:12px;margin:18px 0 8px}
.langlaunch a{display:inline-flex;align-items:center;gap:8px;background:#fff;border:1px solid #D9CFBF;
  border-radius:12px;padding:14px 22px;font-size:15px;font-weight:650;color:#3A2A1E;text-decoration:none;
  transition:transform .16s,box-shadow .16s}
.langlaunch a:hover{transform:translateY(-2px);box-shadow:0 12px 24px -14px rgba(58,42,30,.55)}
.langlaunch a.on{background:#3A2A1E;color:#FFFDF9;border-color:#3A2A1E}
.langlaunch small{font-size:11px;font-weight:500;opacity:.65}
"""

cells = ''.join(
    '<div class="pitem" data-pid="%s"><img src="assets/products/card/%s.png" alt="%s" loading="lazy">'
    '<b>%s</b><span>%s</span></div>'
    % (p['id'], p['id'], PROD_I18N[p['id']]['zh'],
       PROD_I18N[p['id']]['zh'], CAT_ZH[p['cat']]) for p in P)

BLOCK = """<!-- PRODUCTS_START -->
<h2>商品目录 · %d 款真实饮品</h2>
<p class="catnote">用户提供的 17 张商品图（美式咖啡含大小两版）已统一处理成浅米底卡片图，替换全部占位商品：商品列表 9 格、定制页底层 16 格与弹窗大图、购物车 3 行、订单确认 3 行。点击分类导航会把该分类的商品排到前面；点商品卡进入定制页时，弹窗大图与标题跟随所选商品。每款商品的英语、法语、中文名称与分类译文统一配置在 <code>i18n.json</code>，原始素材保存在 <code>assets/products/original/</code>。</p>
<div class="catalog">%s</div>
<!-- PRODUCTS_END -->""" % (len(P), cells)

LANG_BLOCK = """<!-- LANG_START -->
<h2>界面语言 · 三套单语文案</h2>
<p class="catnote">十二屏界面已实现法语、英语、中文三套独立文案，任意时刻只显示一种语言：静态标题、顶部分类、商品名与标签、定制选项、购物车、支付与报错提示、演示控制台、动态提示与页面标题全部随语言切换，切换结果跨页面保存。语言下拉只有三项，按 <span class="kbd">L</span> 可循环切换；也可用地址参数 <code>?lang=en</code>、<code>?lang=fr</code>、<code>?lang=zh</code> 直接指定。下面三个入口分别以对应语言启动完整演示。</p>
<div class="langlaunch">
  <a href="pages/01-home.html?debug=1&amp;lang=en">英语界面 <small>EN</small></a>
  <a href="pages/01-home.html?debug=1&amp;lang=fr">法语界面 <small>FR</small></a>
  <a href="pages/01-home.html?debug=1&amp;lang=zh">中文界面 <small>ZH</small></a>
</div>
<!-- LANG_END -->"""


def replace_once(s, old, new):
    """Only add `new` when it is not already present (keeps reruns idempotent)."""
    return s if new in s else s.replace(old, new, 1)


def patch(path, is_gallery):
    s = open(path).read()
    if '.catalog{' not in s:
        s = s.replace('</style>', CSS + '\n</style>', 1)

    if '<!-- PRODUCTS_START -->' in s:
        s = re.sub(r'<!-- PRODUCTS_START -->.*?<!-- PRODUCTS_END -->', lambda m: BLOCK, s, flags=re.S)
    else:
        anchor = '<footer>' if is_gallery else '<h2>操作方式</h2>'
        s = s.replace(anchor, BLOCK + '\n' + anchor, 1)

    if '<!-- LANG_START -->' in s:
        s = re.sub(r'<!-- LANG_START -->.*?<!-- LANG_END -->', lambda m: LANG_BLOCK, s, flags=re.S)
    else:
        s = s.replace('<!-- PRODUCTS_START -->', LANG_BLOCK + '\n<!-- PRODUCTS_START -->', 1)

    if is_gallery:
        s = s.replace('src="screens/', 'src="preview/')
        s = replace_once(s, '逐屏复刻，画板 1080 × 1920。点击卡片进入对应页面。',
                         '逐屏复刻，画板 1080 × 1920。商品已替换为用户提供的 16 款真实饮品，点击卡片进入对应页面。')
        s = replace_once(s, '三类报错、出餐进度与空闲自动回首页。',
                         '三类报错、出餐进度与空闲自动回首页；商品目录已接入 16 款真实饮品，分类切换会重排商品。')
        s = replace_once(s, '每屏均保留 Figma 原始位图、矢量图标与字体，资源已本地化，可离线打开。',
                         '每屏均保留 Figma 原始布局与字体，商品图/名称已替换为真实目录素材，资源已本地化，可离线打开。')
        s = replace_once(s, '纯静态逐屏复刻版另存于 <code>pages-static/</code>',
                         'Figma 原始占位商品的纯静态复刻版另存于 <code>pages-static/</code>')
        s = replace_once(s, '<b>可交互版本</b>', '<b>可交互版本（含真实商品）</b>')
    else:
        s = replace_once(s, 'INTERACTIVE PROTOTYPE · v1', 'INTERACTIVE PROTOTYPE · v2 · REAL PRODUCTS')
        s = replace_once(s, '空闲计时按设计说明生效。',
                         '空闲计时按设计说明生效，商品图与名称已替换为用户提供的 16 款真实饮品。')
        s = replace_once(s, '<li>顶部分类导航切换，售罄商品给出提示</li>',
                         '<li>16 款真实商品图与名称；分类切换重排卡片</li>\n    <li>顶部分类导航切换，售罄商品给出提示</li>')
        s = replace_once(s, '设计稿仅提供单帧商品列表与一张 Banner，因此未虚构额外商品与广告素材；连续滚动导航以分类切换代替（见交付说明）。',
                         '商品目录来自用户提供的 17 张图，配置在 <code>products.json</code>；原图在 <code>assets/products/original/</code>，卡片图与大图由 <code>tools/build_catalog.py</code> 生成。设计稿仍只提供一张 Banner，轮播第 2/3 张沿用素材库占位图，可在控制台替换。')
        s = replace_once(s, 'Figma 导出基准图在 <code>screens/</code>',
                         'Figma 导出基准图在 <code>screens/</code>，当前版本缩略图在 <code>preview/</code>')
    open(path, 'w').write(s)
    print('updated', os.path.basename(path))


patch(os.path.join(HERE, 'all.html'), True)
patch(os.path.join(HERE, 'demo.html'), False)
shutil.copy2(os.path.join(HERE, 'demo.html'), os.path.join(HERE, 'index.html'))
print('index.html synced from demo.html')
