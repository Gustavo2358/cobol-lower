package io.github.gustavo2358.lower.application;

import java.util.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.model.Ids.*;
import io.github.gustavo2358.lower.domain.*;

/** source-dependencies@1: nominal resources, with no executable uses or objects. */
final class SourceResourceLowering {
    private SourceResourceLowering() {}
    static List<Interactions.Resource> resources(SpInput input,UnitId unit,LocalIds ids,SourceOrigins origins,OriginId unitOrigin) {
        var inventory=input.sourceDependencies();
        if(inventory.availability()==SpInput.Availability.UNAVAILABLE)return List.of();
        var result=new ArrayList<Interactions.Resource>();
        result.add(new Interactions.Resource(new ResourceId(unit.publication(),ids.id("resource","source-inventory",unit.localId(),"inventory")),
            new Interactions.LocalResource("source-dependency-inventory"),unitOrigin,
            Optional.of(new Interactions.ResourceDeclaration(unit,"source-dependencies@1","source."+inventory.availability(),"source.inventory@1",List.of(),List.of()))));
        for(var f:inventory.occurrences()) {
            var origin=origins.source("source-dependency",f.id(),f.provenance());
            result.add(new Interactions.Resource(new ResourceId(unit.publication(),ids.id("resource","source-occurrence",unit.localId(),f.id())),
                new Interactions.LiteralTarget("source-"+f.kind().name().toLowerCase(Locale.ROOT),f.qualification().isEmpty()?"source-member":f.qualification(),f.name(),Interactions.ExactName.INSTANCE,origin),origin,
                Optional.of(new Interactions.ResourceDeclaration(unit,f.artifact().isEmpty()?f.name():f.artifact(),"source."+f.resolution(),"source."+f.authority()+"@1",List.of(),List.of()))));
        }
        for(var gap:inventory.gapCodes())result.add(new Interactions.Resource(new ResourceId(unit.publication(),ids.id("resource","source-gap",unit.localId(),gap)),
            new Interactions.LiteralTarget("source-dependency-gap","source-dependencies@1",gap,Interactions.ExactName.INSTANCE,unitOrigin),unitOrigin,
            Optional.of(new Interactions.ResourceDeclaration(unit,gap,"source.PARTIAL","source.inventory@1",List.of(),List.of()))));
        return List.copyOf(result);
    }
}
