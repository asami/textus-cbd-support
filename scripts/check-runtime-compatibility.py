#!/usr/bin/env python3

import argparse
import json
import re
from pathlib import Path


_RUNTIME_PATH = ("packaging", "car", "runtime", "cncf")
_COMPILATION_COORDINATE = re.compile(
    r"org\.goldenport::goldenport-cncf:([^\"'\s]+)"
)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def _scalar(value: str) -> str:
    candidate = value.strip()
    _require(bool(candidate), "Expected a YAML scalar value")
    if candidate.startswith(('"', "'")):
        if candidate.startswith('"'):
            parsed = json.loads(candidate)
        else:
            _require(candidate.endswith("'"), f"Invalid quoted YAML scalar: {candidate}")
            parsed = candidate[1:-1].replace("''", "'")
        _require(isinstance(parsed, str), f"Expected a string scalar: {candidate}")
        return parsed
    return candidate


def _project_declaration(path: Path) -> tuple[dict, str]:
    text = path.read_text(encoding="utf-8")
    stack: list[tuple[int, str]] = []
    values: dict[str, object] = {}

    for linenumber, rawline in enumerate(text.splitlines(), start=1):
        if not rawline.strip() or rawline.lstrip().startswith("#"):
            continue
        _require("\t" not in rawline, f"Tabs are not supported in {path}:{linenumber}")
        indent = len(rawline) - len(rawline.lstrip(" "))
        content = rawline.strip()

        if content.startswith("- "):
            currentpath = tuple(key for _, key in stack)
            if currentpath == _RUNTIME_PATH + ("tested",):
                values.setdefault("tested", []).append(_scalar(content[2:]))
            elif currentpath == _RUNTIME_PATH + ("excluded",):
                values.setdefault("excluded", []).append(_scalar(content[2:]))
            continue

        match = re.fullmatch(r"([^:]+):(.*)", content)
        _require(match is not None, f"Unsupported YAML line in {path}:{linenumber}")
        key = match.group(1).strip()
        rest = match.group(2).strip()
        while stack and stack[-1][0] >= indent:
            stack.pop()
        currentpath = tuple(entry[1] for entry in stack) + (key,)

        if currentpath == _RUNTIME_PATH + ("minimum",):
            values["minimum"] = _scalar(rest)
        elif currentpath in (
            _RUNTIME_PATH + ("tested",),
            _RUNTIME_PATH + ("excluded",),
        ):
            listkey = currentpath[-1]
            if rest:
                parsed = json.loads(rest)
                _require(isinstance(parsed, list), f"Expected a list for {listkey}")
                _require(
                    all(isinstance(item, str) for item in parsed),
                    f"Expected string entries for {listkey}",
                )
                values[listkey] = parsed
            else:
                values.setdefault(listkey, [])

        if not rest:
            stack.append((indent, key))

    _require(isinstance(values.get("minimum"), str), "Missing CNCF minimum")
    _require(isinstance(values.get("tested"), list), "Missing CNCF tested list")
    _require(isinstance(values.get("excluded"), list), "Missing CNCF excluded list")

    coordinates = _COMPILATION_COORDINATE.findall(text)
    _require(
        len(coordinates) == 1,
        f"Expected one goldenport-cncf compile coordinate, found {len(coordinates)}",
    )
    return values, coordinates[0]


def _matrix(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    _require(isinstance(value, dict), "Compatibility matrix must be a JSON object")
    _require(
        value.get("schema_version") == "textus-cbd.runtime-compatibility-matrix.v1",
        "Unsupported compatibility matrix schema",
    )
    _require(value.get("component") == "textus-cbd-support", "Matrix component mismatch")
    return value


def _validate(project: Path, matrixpath: Path, runtime: str | None, evidenceid: str) -> None:
    declaration, compilationversion = _project_declaration(project)
    matrix = _matrix(matrixpath)
    _require(matrix.get("declaration") == declaration, "project.yaml and matrix declarations differ")

    minimum = declaration["minimum"]
    tested = declaration["tested"]
    excluded = declaration["excluded"]
    _require(len(tested) == len(set(tested)), "CNCF tested versions contain duplicates")
    _require(len(excluded) == len(set(excluded)), "CNCF excluded versions contain duplicates")
    _require(not set(tested).intersection(excluded), "A CNCF version is both tested and excluded")
    _require(minimum not in excluded, "The declared CNCF minimum is excluded")
    _require(minimum in tested, "The declared CNCF minimum is not declared tested")
    _require(
        compilationversion in tested and compilationversion not in excluded,
        "The goldenport-cncf compile coordinate is not a non-excluded tested version",
    )

    evidence = matrix.get("evidence")
    candidates = matrix.get("candidates")
    _require(isinstance(evidence, dict) and evidence, "Matrix evidence is missing")
    _require(isinstance(candidates, list) and candidates, "Matrix candidates are missing")
    versions = [candidate.get("version") for candidate in candidates]
    _require(all(isinstance(version, str) for version in versions), "Candidate version is missing")
    _require(len(versions) == len(set(versions)), "Matrix candidate versions contain duplicates")
    declaredversions = set(tested).union(excluded).union({minimum})
    _require(set(versions) == declaredversions, "Matrix candidates do not exactly cover declared versions")
    byversion = {candidate["version"]: candidate for candidate in candidates}

    for version in tested:
        candidate = byversion[version]
        _require(
            candidate.get("classification") == "tested-compatible",
            f"Tested version {version} is not classified tested-compatible",
        )
    minimumcandidate = byversion[minimum]
    _require(
        minimumcandidate.get("classification") == "tested-compatible",
        "The declared CNCF minimum is not classified tested-compatible",
    )
    for version in excluded:
        candidate = byversion[version]
        _require(candidate.get("classification") == "excluded", f"Excluded version {version} is not excluded")
        _require(bool(candidate.get("reason")), f"Excluded version {version} has no reason")

    for candidate in candidates:
        if candidate.get("classification") == "excluded":
            continue
        evidenceids = candidate.get("evidence_ids")
        _require(
            isinstance(evidenceids, list) and evidenceids,
            f"Compatible version {candidate['version']} has no evidence",
        )
        for candidateevidence in evidenceids:
            _require(candidateevidence in evidence, f"Unknown evidence ID: {candidateevidence}")

    for currentid, record in evidence.items():
        _require(isinstance(record, dict), f"Evidence {currentid} is not an object")
        _require(bool(record.get("command")), f"Evidence {currentid} has no command")
        markers = record.get("success_markers")
        _require(
            isinstance(markers, list) and markers and all(isinstance(item, str) for item in markers),
            f"Evidence {currentid} has no success markers",
        )

    testedlabel = ",".join(tested)
    excludedlabel = ",".join(excluded) if excluded else "none"
    print(
        "RUNTIME_COMPATIBILITY_DECLARATION_OK "
        f"minimum={minimum} tested={testedlabel} excluded={excludedlabel} "
        f"compile={compilationversion}"
    )

    if runtime is not None:
        _require(runtime in byversion, f"Runtime candidate {runtime} is unassessed")
        candidate = byversion[runtime]
        _require(candidate.get("classification") != "excluded", f"Runtime candidate {runtime} is excluded")
        _require(runtime in tested, f"Runtime candidate {runtime} is not declared tested")
        _require(
            evidenceid in candidate.get("evidence_ids", []),
            f"Runtime candidate {runtime} has no {evidenceid} evidence requirement",
        )
        print(
            "RUNTIME_COMPATIBILITY_CANDIDATE_OK "
            f"runtime={runtime} classification={candidate['classification']} evidence={evidenceid}"
        )


def main() -> int:
    projectroot = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(
        description="Check CNCF runtime declarations against the assessed compatibility matrix."
    )
    parser.add_argument("--project", type=Path, default=projectroot / "project.yaml")
    parser.add_argument(
        "--matrix",
        type=Path,
        default=projectroot / "docs/spec/runtime-compatibility-matrix.json",
    )
    parser.add_argument("--runtime")
    parser.add_argument("--evidence", default="representative-sar")
    arguments = parser.parse_args()

    try:
        _validate(
            arguments.project,
            arguments.matrix,
            arguments.runtime,
            arguments.evidence,
        )
        return 0
    except (OSError, json.JSONDecodeError, ValueError) as error:
        print(f"RUNTIME_COMPATIBILITY_CHECK_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
