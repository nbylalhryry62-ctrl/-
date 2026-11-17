#!/usr/bin/env python3
"""Bundle pwa-deploy/ into one self-contained HTML file (CSS and JS inlined, bank included).

    python3 scripts/build_single.py            -> dist/aden-portal.html (open it from anywhere)
    python3 scripts/build_single.py --fragment -> dist/aden-portal.fragment.html (body-only, for hosts that wrap the page)
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, "pwa-deploy")
fragment = "--fragment" in sys.argv
html = open(os.path.join(WEB, "index.html"), encoding="utf-8").read()
read = lambda rel: open(os.path.join(WEB, rel), encoding="utf-8").read()

css = lambda m: "<style>\n" + read(m.group(1)) + "\n</style>"
js = lambda m: "<script>\n" + read(m.group(1)).replace("</script", "<\\/script") + "\n</script>"
html = re.sub(r'<link rel="stylesheet" href="(css/[^"]+)">', css, html)
html = re.sub(r'<script src="(js/[^"]+)"></script>', js, html)
html = re.sub(r'\s*<link rel="(manifest|apple-touch-icon|icon)"[^>]*>', "", html)

if fragment:
    fonts = re.search(r'<link rel="stylesheet" href="https://fonts[^>]*>', html).group(0)
    styles = "\n".join(re.findall(r"<style>.*?</style>", html, re.S))
    body = re.search(r"<body>(.*)</body>", html, re.S).group(1)
    title = re.search(r"<title>(.*?)</title>", html).group(1)
    html = (f"<title>{title}</title>\n{fonts}\n{styles}\n"
            '<script>document.documentElement.lang="ar";document.documentElement.dir="rtl";</script>\n' + body)

out_dir = os.path.join(ROOT, "dist")
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "aden-portal.fragment.html" if fragment else "aden-portal.html")
open(out, "w", encoding="utf-8").write(html)
print(f"{out}: {len(html) / 1e6:.2f} MB")
