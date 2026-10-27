# External Model Edit application

The source CML owns Operations, request/response values and generated factories.
ComponentFactory selects the generated ActionCall adapter in ModelEditOperationJson and holds an
optional application supplied by its supported embedding constructor. There is
no generated-source patch, transport-specific model, request-side admission of
facts, bootstrap document schema or new persistence service.

ModelEditAuthorization binds the authenticated principal to host-selected read
access and the accepted scoped grant. ModelEditOperationJson closes the external
selector/context/edit wire shapes and reuses the existing exact codecs.
ModelEditApplication owns the current immutable session and delegates all edits
to the accepted contextual mutation adapter. Reads project that same candidate;
Review consumes server-owned evidence, and semantic diff consumes an existing
Phase10 admission. An edit makes an older admission visibly stale. No operation
promotes CML or records an approval.

The host constructs and retains one factory for one admitted session. Existing
embedding hosts already own Subsystem and ComponentCreate construction; they
select `new impl.ComponentFactory(application)` before ordinary component
creation and transport registration. The no-argument reflection/plugin path
cannot guess a session: it remains unbound until a host chooses this typed
construction route. This lifetime is local single-writer application state;
durable continuation remains the accepted package/history architecture.

Plugin, Web presentation and MCP share the same generated dispatch. A response
contains typed JSON and escaped existing Web confirmation/Palette rendering.
There is no client-specific authorization or privileged ChatGPT/Codex shortcut.
