#!/usr/bin/env python3
"""Roll the kiosk prototype back to any saved version.

  python3 tools/rollback_refine.py                     # v1-baseline (original 1:1)
  python3 tools/rollback_refine.py v2-refine         # home/shopping-only build
  python3 tools/rollback_refine.py v3-all-refine     # full 12-screen refinement
  python3 tools/rollback_refine.py v4-custom-polish  # before cart-type unification
  python3 tools/rollback_refine.py v5-cart-typography  # before 16-pay redesign
  python3 tools/rollback_refine.py v6-pay-redesign    # before shopping UX (v7)
  python3 tools/rollback_refine.py v7-shopping-ux    # before button unification (v8)

Every versioned file is sha256 verified on restore; tokens.css / refine.css
are removed automatically when restoring the pre-refine v1 snapshot.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import version_manager as VM  # noqa: E402


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else 'v1-baseline'
    VM.restore(name)
    print('\nRestored %s. Open versions/%s/pages/01-home.html to view it.' % (name, name))
    print('Switch versions with: python3 tools/version_manager.py restore <name>')
    print('Available:', ', '.join(
        d.name for d in sorted(VM.VERSIONS.iterdir())
        if d.is_dir() and (d / 'manifest.json').exists()))


if __name__ == '__main__':
    main()
