# Nexus Games native runtime quality pass

This file documents the post-migration quality pass for the universal native engine used by games that do not yet have bespoke modules.

Goals:
- keep every game independently addressable by ID;
- preserve the 17 bespoke native modules;
- improve the shared engine without reintroducing legacy dependencies;
- keep Nexus Store play integration stable;
- progressively replace generic profiles with bespoke implementations.
