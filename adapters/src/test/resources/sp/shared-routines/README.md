# Stage 5 structural sharing probes

Source fixtures generated through real frontend `4c00dea2a6bad1ba21076e55681f6038b80f8a47`, SP 2.62.0, default frontend profile, empty COPY library. JSON is the original SP output. Tests do not fabricate executable topology.

- `shared-cics-stable`: two callers with identical handler state share a body.
- `shared-cics-distinct-state`: a later HANDLE replaces the first; body keys must remain distinct for different states.
- `shared-caller-values`: A and B join only inside the shared CALL; each continuation retains its own value.

| Source | SHA-256 |
| --- | --- |
| shared-caller-values.cbl | `a577f572fea4e498da163b7ac00d6e9e5da831dcd76c85705d66527244b20f40` |
| shared-cics-distinct-state.cbl | `9d7e1e6f626e5fe412d9ded21098aac2394ac533ba00e3d7f1afe811d3a24e14` |
| shared-cics-stable.cbl | `620a534e8cb0255fc54c05916e22fa112aab3fd0d7d1ff21dfc5bfb5b4b96e43` |
