#!/usr/bin/env python3
"""Snapshot / restore complete runnable versions of the kiosk prototype.

  python3 tools/version_manager.py snapshot v1-baseline
  python3 tools/version_manager.py restore  v1-baseline
  python3 tools/version_manager.py list
  python3 tools/version_manager.py verify  v1-baseline

A snapshot is a *runnable* copy: pages/, all shared runtime files, plus an
`assets` symlink back to the real asset tree. So you can open
versions/<name>/pages/01-home.html directly in a browser at any time.

Only files listed in ROOT_FILES + pages/*.html are versioned. Generated
preview PNGs and test scripts are deliberately excluded.
"""
import hashlib
import json
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSIONS = ROOT / 'versions'
MANIFEST = 'manifest.json'

ROOT_FILES = [
    'kiosk.js', 'kiosk.css', 'kiosk-cart.css',
    'i18n.js', 'i18n.json', 'products.json',
    'tokens.css', 'refine.css',
    'demo.html', 'index.html', 'all.html',
    'home-static-1x1.html', 'reference.png',
]
# Files that only exist in the refined version; restoring a version that
# does not contain them removes them from the working root.
OPTIONAL = {'tokens.css', 'refine.css'}


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def targets():
    out = [ROOT / f for f in ROOT_FILES]
    out += sorted((ROOT / 'pages').glob('*.html'))
    return [p for p in out if p.exists()]


def rel(p):
    return str(p.relative_to(ROOT))


def snapshot(name, quiet=False):
    dest = VERSIONS / name
    if dest.exists():
        shutil.rmtree(dest)
    (dest / 'pages').mkdir(parents=True)
    man = {'name': name, 'files': {}}
    for p in targets():
        d = dest / rel(p)
        d.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, d)
        man['files'][rel(p)] = sha(p)
    # Make the archive independently viewable over file://
    link = dest / 'assets'
    if not link.exists():
        link.symlink_to(os.path.relpath(ROOT / 'assets', dest))
    (dest / MANIFEST).write_text(json.dumps(man, ensure_ascii=False, indent=2))
    if not quiet:
        print('snapshot %-16s %d files -> %s' % (name, len(man['files']), dest))
    return dest


def restore(name):
    src = VERSIONS / name
    man = json.loads((src / MANIFEST).read_text())
    missing = [f for f in man['files'] if not (src / f).exists()]
    if missing:
        raise SystemExit('snapshot is incomplete: %s' % missing[:5])
    for f, digest in man['files'].items():
        d = ROOT / f
        d.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src / f, d)
        if sha(d) != digest:
            raise SystemExit('restore hash mismatch: %s' % f)
    # Remove refined-only files that this version does not ship
    for f in OPTIONAL:
        if f not in man['files'] and (ROOT / f).exists():
            (ROOT / f).unlink()
    print('restored %-16s %d files (all hashes verified)' % (name, len(man['files'])))
    print('open versions/%s/pages/01-home.html to view this version directly' % name)


def verify(name):
    src = VERSIONS / name
    man = json.loads((src / MANIFEST).read_text())
    bad = [f for f, h in man['files'].items() if not (src / f).exists() or sha(src / f) != h]
    print('%s: %d files, %d corrupt' % (name, len(man['files']), len(bad)))
    if bad:
        print('  corrupt:', bad[:10])
    return not bad


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'list'
    if not VERSIONS.exists():
        VERSIONS.mkdir(parents=True)
    if cmd == 'snapshot':
        snapshot(sys.argv[2])
    elif cmd == 'restore':
        restore(sys.argv[2])
    elif cmd == 'verify':
        ok = verify(sys.argv[2])
        sys.exit(0 if ok else 2)
    elif cmd == 'list':
        for d in sorted(VERSIONS.iterdir()):
            if d.is_dir() and (d / MANIFEST).exists():
                m = json.loads((d / MANIFEST).read_text())
                print('%-18s %3d files  refine=%s' % (
                    d.name, len(m['files']), 'tokens.css' in m['files']))
    else:
        raise SystemExit('usage: version_manager.py snapshot|restore|verify|list [name]')


if __name__ == '__main__':
    main()
