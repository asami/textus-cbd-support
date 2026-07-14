# Source Authentication Reference Contract

## Boundary

Source authorization and source authentication are independent. Exact-origin
or fixed-route authorization is evaluated before an authentication binding can
be used. A credential never expands the authorized origin, route, or source ID.

`TEXTUS_CBD_SOURCE_AUTHENTICATION` accepts bounded comma-separated bindings:

```text
source-id=scheme:config-key/runtime.configuration.key
```

The supported Phase 4 schemes are `bearer`, `basic`, and `api-key`. The
credential reference must begin with `config-key/` and contain only a bounded
configuration-key identifier. A raw token, password, URI, query, fragment, or
header value is not a valid credential reference.

Bindings are admitted only for already configured catalog, BoK-site, or SIE
source IDs. The first valid binding for a source wins; duplicates, unknown
sources, unsupported schemes, malformed bindings, and invalid references are
rejected with sanitized diagnostics that do not repeat credential material.

## Projection

The runtime source object retains `SourceAuthentication`, containing the scheme
and credential reference needed by the later outbound execution boundary. The
shared information-source descriptor and MCP source-status record expose only:

- `authenticationScheme`: `none`, `bearer`, `basic`, or `api-key`;
- `credentialConfigured`: whether an admitted reference exists.

They never expose `credentialRef`. Runtime environment configuration is not
written into generated CML, CAR metadata, source diagnostics, or MCP output.

## Deferred Resolution

P4-01 defines and validates references only. P4-02 owns late resolution through
the CNCF provider/configuration boundary, authenticated header construction,
source/origin scoping, and CallTree-safe request metadata. No P4-01 code reads a
secret or adds an authentication header.
