package io.github.gustavo2358.lower.adapters.air;

import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.air.model.Ids;
import io.github.gustavo2358.air.model.Publication;
import java.util.*;

/** Independent whole-model equality modulo a bijection of typed IDs, including their owners. */
final class IdentityRenamingOracle {
    private final Map<Ids.Id,Ids.Id> forward=new HashMap<>(), reverse=new HashMap<>();
    private int occurrences;
    static void verify(Publication current) throws Exception {
        byte[] bytes;
        try(var stream=IdentityRenamingOracle.class.getResourceAsStream("/air/previous-local-policy.air.json")) {
            bytes=Objects.requireNonNull(stream).readAllBytes();
        }
        var old=new AirJson().decode(bytes); var oracle=new IdentityRenamingOracle();
        oracle.equal(old,current);
        if(oracle.forward.size()!=20 || oracle.occurrences!=163) throw new AssertionError("RENAMING complete ID traversal: "+oracle.forward.size()+"/"+oracle.occurrences);
        System.out.println("LOCAL_ID_BIJECTION="+oracle.forward.size()+" id_occurrences="+oracle.occurrences+" old_air_bytes="+bytes.length);
    }
    private void equal(Object old,Object now) throws ReflectiveOperationException {
        if(old==null || now==null) { require(old==now,"null");return; }
        require(old.getClass()==now.getClass(),"type");
        if(old instanceof Ids.Id a && now instanceof Ids.Id b) {
            var previous=forward.putIfAbsent(a,b);var inverse=reverse.putIfAbsent(b,a);occurrences++;
            require(previous==null || previous.equals(b),"every reference follows same ID mapping");
            require(inverse==null || inverse.equals(a),"ID map is injective");
        }
        if(old instanceof Optional<?> a && now instanceof Optional<?> b) {
            require(a.isPresent()==b.isPresent(),"optional presence");if(a.isPresent())equal(a.orElseThrow(),b.orElseThrow());
        } else if(old instanceof List<?> a && now instanceof List<?> b) {
            require(a.size()==b.size(),"all occurrences retained");for(int i=0;i<a.size();i++)equal(a.get(i),b.get(i));
        } else if(old.getClass().isRecord()) {
            for(var c:old.getClass().getRecordComponents()) {
                if(old instanceof Ids.Id && c.getName().equals("localId"))continue;
                equal(c.getAccessor().invoke(old),c.getAccessor().invoke(now));
            }
        } else require(old.equals(now),"non-ID fact preserved: "+old);
    }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError("RENAMING "+message); }
}
