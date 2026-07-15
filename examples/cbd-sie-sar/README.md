# CBD Support and SIE Representative SAR

This representative descriptor composes the snapshot CARs for CBD Support and
Textus Semantic Integration Engine as subsystem `textus-cbd-sie`.

Run the complete local build, SAR assembly, temporary CNCF server, and live MCP
probe from the CBD Support project root:

```bash
scripts/check-cbd-sie-sar.sh
```

The check uses CNCF `0.5.1-SNAPSHOT` by default and builds both CARs. For each
profile it creates a temporary `component.d` containing the two CARs and the
selected descriptor as a SAR, starts one owned loopback server, verifies the
exact JSON-RPC `tools/list` set, probes disabled `tools/call` routes, and stops
the server before the next profile.

| profile | descriptor | CBD tools | SIE tools |
|---|---|---:|---:|
| baseline | `subsystem-descriptor.yaml` | 6 | 7 |
| global disabled | `profiles/global-disabled.yaml` | 0 | 0 |
| SIE service disabled | `profiles/sie-service-disabled.yaml` | 6 | 0 |
| status operations disabled | `profiles/operation-disabled.yaml` | 5 | 6 |

Every non-baseline catalog must be an exact subset of the component-declared
baseline. Representative disabled calls must return JSON-RPC invalid-params
code `-32602`. Any administration, mutation, legacy facade, or otherwise
unexpected tool fails the check. The owned server and temporary SAR workspace
are removed on exit.

Override `TEXTUS_SIE_ROOT`, `CNCF_BIN`, `CNCF_VERSION`,
`CNCF_SERVER_PORT`, or `CNCF_HTTP_BASEURL` only when validating another local
checkout or runtime candidate. Set `CNCF_RUNTIME_DEV_DIR` when the candidate is
a local CNCF runtime checkout.
