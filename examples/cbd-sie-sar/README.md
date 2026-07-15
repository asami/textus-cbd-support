# CBD Support and SIE Representative SAR

This representative descriptor composes the snapshot CARs for CBD Support and
Textus Semantic Integration Engine as subsystem `textus-cbd-sie`.

Run the complete local build, SAR assembly, temporary CNCF server, and live MCP
probe from the CBD Support project root:

```bash
scripts/check-cbd-sie-sar.sh
```

For the focused two-main-component composition gate, run:

```bash
scripts/check-cbd-sie-sar.sh --profile baseline
```

The focused command still builds both current CARs, assembles the SAR, starts
the owned CNCF server and fixture server, verifies all 13 read-only tools, and
proves stable `component.service.operation` identities, lexical discovery
order, collision absence, and source ownership. It skips the three
policy-narrowing profiles. Running without `--profile` remains the complete
four-profile matrix.

The check uses CNCF `0.5.1-SNAPSHOT` by default and builds both CARs. For each
profile it creates a temporary `component.d` containing the two CARs and the
selected descriptor as a SAR, starts one owned loopback server, verifies the
exact JSON-RPC `tools/list` set, probes profile-disabled and all 13 non-public
administration, ingestion, rebuild, import, and legacy-facade `tools/call`
routes, and stops the server before the next profile.

Before those builds, `scripts/check-runtime-compatibility.py` requires the
selected `CNCF_VERSION` to match the project declaration and a non-excluded
tested candidate in `docs/spec/runtime-compatibility-matrix.json`. The complete
run emits `RUNTIME_COMPATIBILITY_EXECUTION_OK`; development-directory evidence
also includes its Git revision and clean/dirty state.

| profile | descriptor | CBD tools | SIE tools |
|---|---|---:|---:|
| baseline | `subsystem-descriptor.yaml` | 6 | 7 |
| global disabled | `profiles/global-disabled.yaml` | 0 | 0 |
| SIE service disabled | `profiles/sie-service-disabled.yaml` | 6 | 0 |
| status operations disabled | `profiles/operation-disabled.yaml` | 5 | 6 |

Every non-baseline catalog must be an exact subset of the component-declared
baseline. Every profile-disabled call and every component operation outside the
declared read-only baseline must return JSON-RPC invalid-params code `-32602`.
Any administration, ingestion, mutation, rebuild, import, legacy facade, or
otherwise unexpected tool fails the check. The owned server and temporary SAR
workspace are removed on exit.

The baseline profile also starts a loopback server for the repository-owned
`fixtures/` tree. It configures CBD with the `fixture-catalog` catalog, the
`working` development directory, empty temporary local/cache CAR roots, a
deliberately missing catalog, and the composed SIE `/mcp` route. The probe
ingests `fixtures/bok` through SIE's administration HTTP route before using only
the public composed MCP retrieval surface for its assertions.

The source-aware assertions require the following independent evidence:

- published-catalog `textus-runtime` version `1.0.0` owned by
  `fixture-catalog`;
- development-directory `textus-runtime` version `1.1.0-SNAPSHOT` owned by
  `working`; and
- SIE-owned `architecture:runtime` semantic evidence owned by `semantic`.

The two versions must remain conflict participants with no selected
observation. A `limit=1` request may bound visible observations and semantic
evidence, but must not erase conflict provenance. The missing catalog must stay
degraded with bounded diagnostics and unchanged retry timestamps across an
immediate second search. These assertions do not depend on external catalog
availability.

Override `TEXTUS_SIE_ROOT`, `CNCF_BIN`, `CNCF_VERSION`,
`CNCF_SERVER_PORT`, `CNCF_HTTP_BASEURL`, `CBD_SIE_SAR_FIXTURE_PORT`, or
`CBD_SIE_SAR_FIXTURE_BASEURL` only when validating another local checkout or
runtime candidate. Set `CNCF_RUNTIME_DEV_DIR` when the candidate is a local CNCF
runtime checkout. Both server base URLs must remain loopback addresses.
