# CBD Support Standalone SAR

This one-main-component SAR proves that CBD Support can supply component detail
and usage guidance without loading SIE or copying SIE internal storage.

Run from the repository root:

```sh
scripts/check-cbd-standalone.sh
```

The lifecycle builds the CBD Support CAR, assembles this SAR, and starts CNCF
with an authorized published-catalog fixture. It deliberately omits the SIE
CAR, SIE BoK route, SIE provider configuration, and general BoK source
configuration.

The executable probe requires exactly the seven CBD retrieval tools, no SIE
tools, and no `sie-bok` information source. It resolves exact detail for
`org.textus:textus-runtime:car@1.0.0`, obtains observed usage evidence for
`RuntimeInspection.inspectRuntime`, and verifies that a SIE tool call is
rejected with JSON-RPC `-32602`.
