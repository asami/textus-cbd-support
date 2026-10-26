#!/usr/bin/env -S uv run --no-cache --managed-python --no-python-downloads --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Invoke the compiled external-review test host without SBT or AI execution.

The caller supplies an ordinary Operation input and retains the real external
worker response separately. This helper never creates review findings.
@since   Oct. 27, 2026
@version Oct. 27, 2026
"""

from __future__ import annotations

import argparse
from pathlib import Path
import subprocess


REPOSITORY = Path(__file__).resolve().parents[2]
CLASSPATH = REPOSITORY / "target/streams/test/fullClasspath/_global/streams/export"
BRIDGE = "org.simplemodeling.textus.cbdsupport.ScopedQualityReviewExternalExecutionMain"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True, type=Path)
    parser.add_argument("operation", choices=("prepareScopedQuality", "acceptScopedQuality"))
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    return subprocess.run(
        [str(args.java), "-cp", CLASSPATH.read_text().strip(), BRIDGE,
         args.operation, str(args.input.resolve()), str(args.output.resolve())],
        cwd=REPOSITORY,
        check=False,
    ).returncode


if __name__ == "__main__":
    raise SystemExit(main())
