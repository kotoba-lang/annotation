# ADR 0001: Use Kotoba bounded documents as the source and runtime contract

Status: accepted

## Context

The former `src/annotation/core.cljc` library returned unrestricted Clojure
maps and depended on host collection semantics. Renaming that file would not
make it portable or safe: its values were heterogeneous, extensible W3C
documents, while Kotoba's general `:map` intentionally stores only bounded
keyword-to-i64 entries.

## Decision

Replace the `.cljc` implementation with `src/annotation/core.kotoba` and use
Kotoba typed ABI v11 `:document` throughout. Documents are bounded canonical
tagged trees, and all construction, lookup, merge, validation, and update work
is performed by admitted pure primitives. The required JSON-LD `@context` key
uses bounded `keyword-from-string`; it is not renamed to a more convenient but
incorrect key.

The build-time compiler is pinned by immutable Git SHA. Produced restricted
JavaScript and Wasm modules have no JVM runtime dependency. Tests execute the
reference semantics and instantiate both generated targets, rather than
asserting source renaming alone.

## Consequences

Host maps, objects, getters, functions, cyclic graphs, non-finite floats,
oversized documents, and forged Wasm externrefs are rejected. Optional fields
are represented by omission. The library's input and output contract is now
`:document`, so callers must migrate at the boundary instead of relying on
ambient Clojure map operations.
