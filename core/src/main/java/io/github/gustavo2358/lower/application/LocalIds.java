package io.github.gustavo2358.lower.application;

import java.util.HashMap;
import java.util.Map;

/** One publication's exact collision registry; descriptors retain original strings, never encoded copies. */
final class LocalIds {
    static final String POLICY = "local-xxh3-128-v1";
    private record Identity(String namespace, String role, String owner, String key) { }
    private final Map<String, Identity> registered = new HashMap<>();

    String id(String namespace, String role, String owner, String key) {
        var identity = new Identity(namespace, role, owner, key);
        String digest = CanonicalRevision.local(namespace, role, owner, key);
        var previous = registered.putIfAbsent(digest, identity);
        if (previous != null && !previous.equals(identity))
            throw new IllegalStateException("LOCAL_ID_COLLISION: distinct identities share " + digest);
        return digest;
    }
}
