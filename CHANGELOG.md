# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `UniState` and `UniAction` marker interfaces that `State` and `Action` types implement.
- `@UniDsl` (a `@DslMarker`) on the DSL receiver, which prevents a DSL block from accidentally reaching an outer DSL's receiver when blocks are nested.
- KDoc on `effect`, `reducer`, and both `uniViewModelDSL` builder overloads.

### Changed
- **Breaking:** the type parameters of `UnidirectionalViewModel`, `UniViewModel`, `DslUnidirectionalViewModel`, `SideEffect`, and the `uniViewModelDSL` builders are now bound to `UniState` / `UniAction`. Existing `State` and `Action` types must implement these interfaces.
- `uniViewModelDSL` now requires a `reducer`. Building without one fails fast with a clear message instead of silently leaving state unchanged.
