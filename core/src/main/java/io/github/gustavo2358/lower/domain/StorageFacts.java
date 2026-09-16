package io.github.gustavo2358.lower.domain;

import java.math.BigInteger;
import java.util.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Source-owned physical facts, not an AIR memory model. Semantic validity is checked at admission. */
public final class StorageFacts {
    private StorageFacts() { }
    public static final String PROFILE_ID="ibm-enterprise-6.4-fixed-display-1047@1";
    public static final String CODEC="text.ebcdic.ibm1047@1";
    public enum Profile { UNSPECIFIED, IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047 }
    public enum Kind { GROUP, ELEMENTARY, OPAQUE }
    public enum Allocation { INDEPENDENT_LOCAL_WORKING_STORAGE, UNPROVEN }
    public enum MoveKind { LITERAL_BYTES, FITTED_LITERAL_BYTES, COPY_BYTES, FIT_TEXT, MUST_UNKNOWN, UNAVAILABLE }
    public enum RelationStatus { PROVEN, UNPROVEN }
    public record NodeId(UnitKey unit,String handle) { public NodeId { Objects.requireNonNull(unit);Objects.requireNonNull(handle); } }
    public record BaseId(UnitKey unit,String handle) { public BaseId { Objects.requireNonNull(unit);Objects.requireNonNull(handle); } }
    public record RelationId(UnitKey unit,String handle) { public RelationId { Objects.requireNonNull(unit);Objects.requireNonNull(handle); } }
    public record Relation(RelationId id,NodeId owner,Optional<NodeId> target,RelationStatus status,Provenance provenance,List<String> gapCodes) {
        public Relation { Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(target);Objects.requireNonNull(status);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes); }
    }
    public record Renames(RelationId id,NodeId owner,Optional<NodeId> from,Optional<NodeId> through,
            RelationStatus status,Provenance provenance,List<String> gapCodes) {
        public Renames { Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(from);Objects.requireNonNull(through);Objects.requireNonNull(status);Objects.requireNonNull(provenance);gapCodes=List.copyOf(gapCodes); }
    }
    public record Measure(Optional<BigInteger> value,List<String> gapCodes) {
        public Measure { Objects.requireNonNull(value);gapCodes=List.copyOf(gapCodes); }
    }
    public record Node(NodeId id,Optional<NodeId> parent,int order,boolean filler,Kind kind,Optional<DataId> data,
            Measure extent,Provenance provenance) {
        public Node { Objects.requireNonNull(id);Objects.requireNonNull(parent);Objects.requireNonNull(kind);Objects.requireNonNull(data);Objects.requireNonNull(extent);Objects.requireNonNull(provenance); }
    }
    public record Base(BaseId id,Measure extent,Allocation allocation,Provenance provenance) {
        public Base { Objects.requireNonNull(id);Objects.requireNonNull(extent);Objects.requireNonNull(allocation);Objects.requireNonNull(provenance); }
    }
    public record View(NodeId node,BaseId base,Measure offset,Measure extent,Optional<String> codec,Provenance provenance) {
        public View { Objects.requireNonNull(node);Objects.requireNonNull(base);Objects.requireNonNull(offset);Objects.requireNonNull(extent);Objects.requireNonNull(codec);Objects.requireNonNull(provenance); }
    }
    public record Slice(BigInteger offset,BigInteger extent) {
        public Slice { Objects.requireNonNull(offset);Objects.requireNonNull(extent); }
    }
    public record Access(NodeId view,Optional<Slice> slice) {
        public Access { Objects.requireNonNull(view);Objects.requireNonNull(slice); }
        public Access(NodeId view) { this(view,Optional.empty()); }
    }
    public record Move(MoveKind kind,List<Integer> bytes,List<String> gapCodes) {
        public Move { Objects.requireNonNull(kind);bytes=List.copyOf(bytes);gapCodes=List.copyOf(gapCodes); }
    }
    public enum EntryMode { UNKNOWN, INITIAL, PRESERVED }
    public enum InitialKind { LITERAL_BYTES, POSSIBLE_LITERAL_BYTES, PRESERVE, UNKNOWN }
    public enum InitialProof { NONE, EXPLICIT_INITIAL, EXPLICIT_PRESERVED, PROGRAM_INITIAL, DECLARATIVE_INVARIANT, DECLARATIVE_POSSIBILITY }
    public record InitialCondition(NodeId node,InitialKind kind,List<Integer> bytes,List<String> gapCodes,Provenance provenance,InitialProof proof) {
        public InitialCondition {Objects.requireNonNull(proof);Objects.requireNonNull(node);Objects.requireNonNull(kind);bytes=List.copyOf(bytes);gapCodes=List.copyOf(gapCodes);Objects.requireNonNull(provenance);}
        /** Historic storage 1.3 facts only asserted literal values under explicit INITIAL. */
        public InitialCondition(NodeId node,InitialKind kind,List<Integer> bytes,List<String> gapCodes,Provenance provenance) {
            this(node,kind,bytes,gapCodes,provenance,kind==InitialKind.UNKNOWN?InitialProof.NONE:kind==InitialKind.PRESERVE?InitialProof.EXPLICIT_PRESERVED:kind==InitialKind.POSSIBLE_LITERAL_BYTES?InitialProof.DECLARATIVE_POSSIBILITY:InitialProof.EXPLICIT_INITIAL);
        }
    }
    public enum PossibilityDomain { BOUNDED_PHYSICAL, LOGICAL_SOURCE }
    public record EntryState(EntryMode mode,List<InitialCondition> conditions,PossibilityDomain possibilityDomain) {
        public EntryState {Objects.requireNonNull(possibilityDomain);Objects.requireNonNull(mode);conditions=List.copyOf(conditions);}
        public EntryState(EntryMode mode,List<InitialCondition> conditions) {this(mode,conditions,PossibilityDomain.BOUNDED_PHYSICAL);}
        public static EntryState unknown() {return new EntryState(EntryMode.UNKNOWN,List.of());}
    }
    public record Inventory(Profile profile,Optional<String> profileId,Optional<String> runtimeCodec,List<Node> nodes,
            List<Base> bases,List<View> views,List<String> gapCodes,List<Relation> relations,List<Renames> renames,EntryState entryState) {
        public Inventory {
            Objects.requireNonNull(entryState);Objects.requireNonNull(profile);Objects.requireNonNull(profileId);Objects.requireNonNull(runtimeCodec);
            nodes=List.copyOf(nodes);bases=List.copyOf(bases);views=List.copyOf(views);gapCodes=List.copyOf(gapCodes);relations=List.copyOf(relations);renames=List.copyOf(renames);
        }
        public Inventory(Profile profile,Optional<String> profileId,Optional<String> runtimeCodec,List<Node> nodes,List<Base> bases,List<View> views,List<String> gapCodes,List<Relation> relations,List<Renames> renames) {
            this(profile,profileId,runtimeCodec,nodes,bases,views,gapCodes,relations,renames,EntryState.unknown());
        }
        public Inventory(Profile profile,Optional<String> profileId,Optional<String> runtimeCodec,List<Node> nodes,List<Base> bases,List<View> views,List<String> gapCodes,List<Relation> relations) {
            this(profile,profileId,runtimeCodec,nodes,bases,views,gapCodes,relations,List.of());
        }
        public Inventory(Profile profile,Optional<String> profileId,Optional<String> runtimeCodec,List<Node> nodes,List<Base> bases,List<View> views,List<String> gapCodes) {
            this(profile,profileId,runtimeCodec,nodes,bases,views,gapCodes,List.of());
        }
    }
}
