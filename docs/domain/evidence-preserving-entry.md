# EP-W1 — logical source evidence

Authority: SP 2.19.0/storage 1.6.0 and AIR entry.possibilities@2. W0 authority
map precedes implementation. Source recognition remains exclusively upstream.

The input port distinguishes BOUNDED_PHYSICAL entry possibilities (legacy) from
LOGICAL_SOURCE possibilities. This distinction is part of canonical identity.
The latter requires source provenance, bytes with a selected profile or logical
text, and lifecycle remainder;
physical bounds and independent allocation are separate precision obligations.
Legacy inputs retain their strict checks; storage 1.6 is negotiated only by SP2.19.

`POSSIBLE_LOGICAL_TEXT` transports the recognized logical candidate independently
of whether a byte profile is selected. It requires nullable-field presence, a logical text value,
no bytes, source provenance and remainder. Lowering maps it directly to AIR TEXT
PossibleLiterals on the logical object, without selecting a codec. Other kinds
have null logicalText. A selected profile does not force this logical fact into a
physical region or a BYTES domain. Older SP versions cannot introduce the field or kind.

`logicalWholeItem` identifies a source-proved complete logical access, independent
of physical layout. It agrees with the resolved declaration and carries exact
provenance. It does not create a scalar storage guarantee. An unknown physical
declaration can therefore receive a known TEXT logical type from source entry
evidence, an UnknownBinding, and a DataLink whose storage is absent. No Cell,
extent, lifetime or disjoint premise is synthesized. CALL reads that object;
entry evidence is translated to PossibleLiterals with its required remainder.

Translation traverses declared facts and preserves deterministic source IDs;
it performs no source parsing or dataflow. Unresolved references, subscripts and
recuts without an upstream access proof remain partial. Known physical slices
retain their existing byte representation and admission.

A PRESERVED lifecycle does not assert the declaration is the mandatory entry
value: it retains that source-supported possibility alongside unknown prior
content. Strong LITERAL_BYTES requires proved independent allocation as well as
physical bounds and the appropriate lifecycle/invariant proof.

Focused evidence: nine actual producer fixtures cover unknown allocation, base
extent, offset, profile, preserved lifecycle, INITIAL with open allocation,
strong invariant and exact overwrite. Entry/target/AIR codec, RD BEFORE, Regional
Values BEFORE, dependency site and JSON checks passed. Legacy possible-entry six
negative controls and INITIAL/PRESERVED storage controls remain enforced. W2–W5
kill policy, scoped uncertainty and failure containment qualification remain open.
REAL CASE = NOT AVAILABLE.
