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

## Outbound Resolution

Catalog, BoK-site, and SIE providers pass the owning source through source-aware
fetcher/transport methods. Compatibility overloads remain available to test and
in-memory providers, but the production `CbdHttp` path never infers source
ownership from a URI or selects a credential by origin alone.

`CbdHttp` resolves `config-key/...` only inside `ProviderCall.build_Program`,
immediately before the CNCF `http_get` or `http_post` operation is constructed.
Resolution uses `provider_config_string`, so the value comes from CNCF resolved
runtime parameters rather than request properties, environment access in the
transport, or a direct secret-store client. A source without an authentication
binding performs no credential lookup.

Before lookup, the request URI must have the same normalized origin as the
owning source and must not contain URI user information. A failed origin check
does not invoke the resolver. The supported header mappings are:

- `bearer`: `Authorization: Bearer <credential>`;
- `basic`: `Authorization: Basic <credential>`, where the resolved value is the
  pre-encoded Basic credential payload;
- `api-key`: `X-Api-Key: <credential>`.

Empty values and values containing control characters are rejected without
including the credential or configuration key in the failure. Provider and
authentication CallTree attributes contain only the sanitized request URI,
source ID, authentication scheme, and configured-state flag. They never contain
the credential reference, resolved value, or authentication header.

P4-03 owns distinct missing, unavailable, expired, and rejected credential
failure classifications. P4-04 owns the complete executable security matrix
for isolation, redaction, and CallTree metadata.
