#!/usr/bin/env python3

import argparse
import json
import zipfile
from pathlib import Path


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def _read_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    _require(isinstance(value, dict), f"Expected a JSON object: {path}")
    return value


def _metadata_operations(metadata: dict) -> list[dict]:
    component = metadata.get("surface", {}).get("component", {})
    services = component.get("services", [])
    _require(isinstance(services, list), "Model metadata services must be a list")
    operations: list[dict] = []
    for service in services:
        serviceoperations = service.get("operations", [])
        _require(isinstance(serviceoperations, list), "Service operations must be a list")
        for operation in serviceoperations:
            record = {
                "name": operation.get("name"),
                "kind": operation.get("operationType"),
                "input": operation.get("inputType"),
                "output": operation.get("outputType"),
            }
            _require(
                all(isinstance(value, str) and value for value in record.values()),
                f"Model metadata operation is incomplete: {operation}",
            )
            operations.append(record)
    names = [operation["name"] for operation in operations]
    _require(len(names) == len(set(names)), "Model metadata operation names are not unique")
    return operations


def _metadata_entities(metadata: dict) -> list[dict]:
    elements = metadata.get("modelElements", [])
    _require(isinstance(elements, list), "Model metadata elements must be a list")
    entities = [
        {"name": element.get("name"), "fields": []}
        for element in elements
        if element.get("kind") == "entity"
    ]
    _require(
        all(isinstance(entity["name"], str) and entity["name"] for entity in entities),
        "Model metadata entity name is missing",
    )
    names = [entity["name"] for entity in entities]
    _require(len(names) == len(set(names)), "Model metadata entity names are not unique")
    return entities


def _archive_manifest(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        _require("abi-manifest.json" in archive.namelist(), "CAR has no top-level ABI manifest")
        value = json.loads(archive.read("abi-manifest.json"))
    _require(isinstance(value, dict), "Packaged ABI manifest must be a JSON object")
    return value


def _validate(manifestpath: Path, metadatapath: Path, archivepath: Path) -> None:
    manifest = _read_json(manifestpath)
    metadata = _read_json(metadatapath)
    _require(
        manifest.get("format") == "cozy.car.abi-manifest.v1",
        "Unsupported current CAR ABI manifest format",
    )
    _require(
        metadata.get("schema") == "cozy.cml.model-metadata.v1",
        "Unsupported CML model metadata schema",
    )

    car = manifest.get("car", {})
    abi = manifest.get("abi", {})
    exports = abi.get("exports", {})
    _require(isinstance(car.get("name"), str) and car.get("name"), "CAR ABI name is missing")
    _require(isinstance(car.get("version"), str) and car.get("version"), "CAR ABI version is missing")
    _require(abi.get("version") == 1, "Unsupported CAR ABI surface version")
    _require(
        exports.get("components") == [{"name": car["name"]}],
        "Current ABI component export does not match the CAR identity",
    )

    expectedoperations = _metadata_operations(metadata)
    expectedentities = _metadata_entities(metadata)
    _require(expectedoperations, "CML model metadata exposes no operations")
    _require(
        exports.get("operations") == expectedoperations,
        "Current ABI operations differ from generated CML model metadata",
    )
    _require(
        exports.get("entities") == expectedentities,
        "Current ABI entities differ from generated CML model metadata",
    )
    _require(isinstance(abi.get("dependencies"), list), "CAR ABI dependencies must be a list")

    packaged = _archive_manifest(archivepath)
    _require(packaged == manifest, "Packaged CAR ABI manifest differs from the source-managed current manifest")
    print(
        "CAR_ABI_SURFACE_OK "
        f"component={car['name']} version={car['version']} "
        f"operations={len(expectedoperations)} entities={len(expectedentities)}"
    )
    print(f"CAR_ABI_PACKAGE_MATCH_OK archive={archivepath.name}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check the source-managed CAR ABI against CML metadata and the packaged CAR."
    )
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--model-metadata", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    arguments = parser.parse_args()

    try:
        _validate(arguments.manifest, arguments.model_metadata, arguments.archive)
        return 0
    except (OSError, json.JSONDecodeError, ValueError, zipfile.BadZipFile) as error:
        print(f"CAR_ABI_SURFACE_CHECK_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
