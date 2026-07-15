# CBD Support and SIE Representative SAR

This representative descriptor composes the snapshot CARs for CBD Support and
Textus Semantic Integration Engine as subsystem `textus-cbd-sie`.

Run the complete local build, SAR assembly, temporary CNCF server, and live MCP
probe from the CBD Support project root:

```bash
scripts/check-cbd-sie-sar.sh
```

The check uses CNCF `0.5.1-SNAPSHOT` by default, builds both CARs, creates a
temporary `component.d` containing the two CARs and this descriptor as a SAR,
and starts one loopback CNCF server. Its JSON-RPC `tools/list` result must equal
the six CBD retrieval tools plus the seven SIE semantic-retrieval tools. Any
administration, mutation, legacy facade, or otherwise unexpected tool fails the
check. The owned server and temporary SAR workspace are removed on exit.

Override `TEXTUS_SIE_ROOT`, `CNCF_BIN`, `CNCF_VERSION`,
`CNCF_SERVER_PORT`, or `CNCF_HTTP_BASEURL` only when validating another local
checkout or runtime candidate.
