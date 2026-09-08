package io.github.gustavo2358.lower.adapters.cli;

/** Bounded composition policy, measured against merged 4A shared-DATA/10000-MOVE SP.
 * Observed: 18809400 bytes, 1050154 JSON nodes, 190021 admission visits, depth 8
 * including leaves. Headroom: approximately 78%, 43%, 32%; no nesting increase.
 * Public flags/configuration and AIR transport limits are separate future decisions.
 */
final class ProductionLimits {
    static final int SP_BYTES = 32 * 1024 * 1024;
    static final int SP_DEPTH = 64;
    static final int SP_NODES = 1_500_000;
    static final int ADMISSION_ENTITIES = 250_000;
    private ProductionLimits() { }
}
