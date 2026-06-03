#!/usr/bin/env python3
"""
Verify flatpak-sources.json: every pinned URL must serve bytes whose sha512
matches the recorded value. Catches URL/hash mismatches (e.g. a Maven Central
artifact pinned with a compose/dev URL) at generation time instead of 40 minutes
into a flatpak-builder run.

Usage: verify-sources.py [flatpak-sources.json]
Exit 0 = all good; exit 1 = at least one mismatch or download failure.
"""

import hashlib
import json
import ssl
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

SSL_CTX = ssl.create_default_context()


def check(entry):
    url = entry.get("url")
    want = entry.get("sha512")
    if not url or not want:
        return None
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "flatpak-verify/1.0"})
        with urllib.request.urlopen(req, timeout=30, context=SSL_CTX) as resp:
            data = resp.read()
    except Exception as e:
        return (url, f"download failed: {e}")
    got = hashlib.sha512(data).hexdigest()
    if got != want:
        return (url, f"sha512 mismatch (pinned {want[:16]}…, server {got[:16]}…)")
    return None


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / "flatpak-sources.json"
    with open(path) as f:
        entries = json.load(f)

    files = [e for e in entries if e.get("type") == "file" and e.get("url") and e.get("sha512")]
    total = len(files)
    print(f"Verifying {total} pinned sources from {path}...")

    problems = []
    with ThreadPoolExecutor(max_workers=16) as ex:
        for i, result in enumerate(ex.map(check, files), 1):
            if i % 200 == 0:
                print(f"  [{i}/{total}] {len(problems)} problem(s) so far")
            if result:
                problems.append(result)

    if problems:
        print(f"\n{len(problems)} PROBLEM(S):", file=sys.stderr)
        for url, msg in problems:
            print(f"  {msg}\n    {url}", file=sys.stderr)
        sys.exit(1)

    print(f"OK — all {total} sources verified.")


if __name__ == "__main__":
    main()
