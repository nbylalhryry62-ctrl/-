#!/usr/bin/env python3
"""Replace the default Flutter icons in the generated android/ios/web folders with the project's icon.
Run from flutter_app/ after `flutter create`. Needs Pillow."""
import os, re, shutil, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICONS = os.path.join(ROOT, "pwa-deploy", "icons")
SRC = Image.open(os.path.join(ICONS, "icon-512.png")).convert("RGBA")
MASK = Image.open(os.path.join(ICONS, "icon-maskable-512.png")).convert("RGBA")

def save(img, size, path, flat=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = img.resize((size, size), Image.LANCZOS)
    if flat:                      # iOS icons must not have transparency
        bg = Image.new("RGB", out.size, (11, 30, 49)); bg.paste(out, mask=out.split()[3]); out = bg
    out.save(path)

# web
if os.path.isdir("web"):
    save(SRC, 16, "web/favicon.png"); 
    for n, im, s in [("Icon-192", SRC, 192), ("Icon-512", SRC, 512), ("Icon-maskable-192", MASK, 192), ("Icon-maskable-512", MASK, 512)]:
        save(im, s, f"web/icons/{n}.png")
    save(SRC, 180, "web/icons/apple-touch-icon.png")
    idx = open("web/index.html", encoding="utf-8").read()
    idx = re.sub(r"<title>.*?</title>", "<title>بوابة القبول - جامعة عدن</title>", idx)
    idx = re.sub(r'(<meta name="apple-mobile-web-app-title" content=")[^"]*', r"\1بوابة القبول", idx)
    idx = re.sub(r'<link rel="apple-touch-icon" href="[^"]*">', '<link rel="apple-touch-icon" href="icons/apple-touch-icon.png">', idx)
    idx = idx.replace('<html', '<html lang="ar" dir="rtl"', 1) if "<html lang" not in idx else idx
    idx = re.sub(r'(<meta name="description" content=")[^"]*', r"\1بنك أسئلة القبول والاختبارات التجريبية - جامعة عدن", idx)
    open("web/index.html", "w", encoding="utf-8").write(idx)
    mf = open("web/manifest.json", encoding="utf-8").read()
    mf = re.sub(r'"name": "[^"]*"', '"name": "بوابة القبول الذكية - جامعة عدن"', mf)
    mf = re.sub(r'"short_name": "[^"]*"', '"short_name": "بوابة القبول"', mf)
    mf = re.sub(r'"background_color": "[^"]*"', '"background_color": "#0B1E31"', mf)
    mf = re.sub(r'"theme_color": "[^"]*"', '"theme_color": "#0A7A62"', mf)
    mf = re.sub(r'"lang": "[^"]*"', '"lang": "ar"', mf) if '"lang"' in mf else mf.replace('"name"', '"lang": "ar",\n    "dir": "rtl",\n    "name"', 1)
    open("web/manifest.json", "w", encoding="utf-8").write(mf)

# android
for d in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
    src = os.path.join(ROOT, "android-build", "icons", f"mipmap-{d}", "ic_launcher.png")
    dst = f"android/app/src/main/res/mipmap-{d}/ic_launcher.png"
    if os.path.exists(src) and os.path.isdir(os.path.dirname(dst)):
        shutil.copy(src, dst)
if os.path.isfile("android/app/src/main/AndroidManifest.xml"):
    m = open("android/app/src/main/AndroidManifest.xml", encoding="utf-8").read()
    m = re.sub(r'android:label="[^"]*"', 'android:label="بوابة القبول"', m, count=1)
    open("android/app/src/main/AndroidManifest.xml", "w", encoding="utf-8").write(m)

# ios
iset = "ios/Runner/Assets.xcassets/AppIcon.appiconset"
if os.path.isdir(iset):
    import json
    cj = json.load(open(os.path.join(iset, "Contents.json")))
    for im in cj["images"]:
        px = round(float(im["size"].split("x")[0]) * int(im["scale"].replace("x", "")))
        save(SRC, px, os.path.join(iset, im["filename"]), flat=True)
    plist = "ios/Runner/Info.plist"
    t = open(plist, encoding="utf-8").read()
    t = re.sub(r"(<key>CFBundleDisplayName</key>\s*<string>)[^<]*", r"\1بوابة القبول", t)
    t = re.sub(r"(<key>CFBundleName</key>\s*<string>)[^<]*", r"\1بوابة القبول", t)
    open(plist, "w", encoding="utf-8").write(t)
print("icons applied")
