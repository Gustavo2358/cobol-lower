package io.github.gustavo2358.lower.adapters.testing;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gustavo2358.air.model.*;
import io.github.gustavo2358.air.json.AirJson;
import io.github.gustavo2358.lower.adapters.cli.CobolLower;
import io.github.gustavo2358.lower.application.*;
import io.github.gustavo2358.lower.domain.*;
import static io.github.gustavo2358.lower.domain.SpInput.*;

/** Two independent authorities: source control may advance while executable AIR stops. */
public final class ControlStorageDecouplingSuite {
    static int checks,metamorphics;
    static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static ObjectNode wire(String name)throws Exception {try(var in=ControlStorageDecouplingSuite.class.getResourceAsStream("/sp/control-effects-r7-r7b1/"+name+".json")){return (ObjectNode)TerminalSendSuite.J.readTree(Objects.requireNonNull(in,name));}}
    static LoweringResult lower(ObjectNode j)throws Exception{return TerminalSendSuite.lower(j);}
    static Set<String> control(SpInput in) {
        var id=TerminalSendSuite.command(in).header().id().handle();var out=new TreeSet<String>();
        in.controlTopology().orElseThrow().outcomes().stream().filter(o->o.statement().equals(id)).forEach(o->out.add(o.kind()+":"+o.role()+":"+o.target().kind()));return out;
    }
    static void bounded(LoweringResult r)throws Exception {
        need(r.status()==LoweringResult.Status.BOUNDED_PUBLICATION,"positive publication retained");
        need(r.validation().orElseThrow().isStructurallyValid(),"strict AIR");
        var input=r.admission().input().orElseThrow();var pub=r.publication().orElseThrow();
        need(TerminalSendSuite.command(input).executableLowering()==ExecutableLowering.NOT_READY,"source control does not qualify executable effects");
        for(var cap:r.admission().nonExecutableCapabilities()) {
            need(cap.executableLowering()==ExecutableLowering.NOT_READY,"capability retained");
            var links=r.statements().stream().filter(l->l.source().equals(cap.statement())).toList();
            need(!links.isEmpty(),"positive occurrence retained");
            for(var link:links) {
                var seq=pub.units().stream().flatMap(u->u.sequences().stream()).filter(s->s.terminator().header().id().equals(link.target())).findFirst().orElseThrow();
                need(seq.instructions().isEmpty(),"no fake executable SEND instruction");
                need(seq.terminator() instanceof Operations.Opaque,"no executable SEND or dispatch");var o=(Operations.Opaque)seq.terminator();
                need(o.envelope().control().known().isEmpty(),"source successor must not become AIR successor");
                need(o.envelope().control().remainder() instanceof Scopes.WithinControl w&&w.scope() instanceof Scopes.LabelsControl ls&&ls.labels().isEmpty(),"no bounded executable destination or AllControl");
                need(o.envelope().memory().knownReads().isEmpty()&&o.envelope().memory().knownWrites().isEmpty()&&o.envelope().memory().mustOverwrite().isEmpty(),"no invented storage/effects");
            }
        }
        need(pub.uncertainties().stream().anyMatch(u->u.code().equals("EXECUTABLE_CAPABILITY_NOT_READY")),"storage/effect frontier explicit");
        need(new CobolLowerer().lower(input,CobolLower.OPTIONS).status()==LoweringResult.Status.IMPLEMENTATION_LIMIT,"historical opt-in boundary");
        new AirJson().decode(new AirJson().encode(pub));
    }
    static HandlerStateAnalysis.Event canceled(String name)throws Exception {
        var j=wire(name);var r=lower(j);bounded(r);var a=r.admission().handlerState().orElseThrow();var e=a.events().getFirst();
        need(e.status()==HandlerStateAnalysis.EventStatus.ASSESSED,"source ABEND reached after qualified SEND "+name);
        need(e.before().stream().allMatch(s->s.state().kind()==HandlerStateAnalysis.Kind.CANCELED||s.state().kind()==HandlerStateAnalysis.Kind.CANCELED_UNKNOWN),"CANCEL must kill active local state");
        need(e.candidates().isEmpty()&&!e.unknownLocalRemainder(),"canceled local target is not an active dispatch candidate");
        need(e.localInactivePossible()&&e.outerLevelRemainder(),"inactive is not global absence; outer levels not erased");
        need(!e.bypassed(),"HANDLE CANCEL is not ABEND CANCEL");
        need(a.equals(HandlerStateScheduleProbe.reverse(r.admission().input().orElseThrow())),"schedule-independent fixed point");return e;
    }
    public static void main(String[] args)throws Exception {
        checks=0;metamorphics=0;
        for(var name:List.of("group","group-array","group-extra","real-derived","changed-handler","cancel-entry","replace-cancel","reset-cancel","cancel-noop","noop-cancel"))canceled(name);
        var base=TerminalSendSuite.input(wire("group-array"));var expected=Set.of("NORMAL:normal:OCCURRENCE");need(control(base).equals(expected),"unknown group storage still positive source control");
        need(!wire("group-array").path("storage").path("logicalExactViews").toString().equals(wire("group").path("storage").path("logicalExactViews").toString()),"real storage variation present");
        for(var name:List.of("group","group-extra","renamed","real-derived","changed-handler"))need(control(TerminalSendSuite.input(wire(name))).equals(expected),"layout/rename/dead-storage/handler target independent");
        metamorphics+=4; // layout, declaration rename, dead storage, handler target
        var enabled=wire("views-enabled");var disabled=wire("views-disabled");var physical=wire("views-physical");
        need(enabled.path("storage").path("logicalTextViews").size()>0&&disabled.path("storage").path("logicalTextViews").isEmpty(),"add/remove existing logical group view exercised");
        need(!physical.path("storage").path("views").equals(disabled.path("storage").path("views")),"physical materializability differs under explicit test-only profile");
        for(var w:List.of(enabled,disabled,physical)) {
            need(w.path("controlTopology").equals(enabled.path("controlTopology")),"same source identity/control, different storage proof");
            var r=lower(w);bounded(r);need(r.admission().handlerState().equals(lower(enabled).admission().handlerState()),"source state unaffected by storage availability");
        }metamorphics+=3; // remove group view, add group view, AIR materializability
        var no=TerminalSendSuite.input(wire("no-nohandle"));need(control(no).contains("UNKNOWN_LOCAL:cics/handler-or-default-condition:UNKNOWN_LOCAL"),"NOHANDLE absence retains condition remainder");
        need(!control(base).stream().anyMatch(s->s.startsWith("UNKNOWN")),"explicit NOHANDLE returns modeled conditions locally");metamorphics++;
        var map=TerminalSendSuite.input(wire("map"));need(TerminalSendSuite.command(map).commandKind()==CicsCommandKind.SEND_MAP&&control(map).stream().anyMatch(s->s.contains("overflow")),"MAP uses its own condition rule");metamorphics++;
        var reversed=wire("group-array");var array=(ArrayNode)reversed.path("statements");var list=new ArrayList<JsonNode>();array.forEach(list::add);Collections.reverse(list);array.removeAll();list.forEach(array::add);
        var original=lower(wire("group-array"));var reordered=lower(reversed);need(original.admission().handlerState().equals(reordered.admission().handlerState()),"inventory order not execution authority");
        need(Arrays.equals(new AirJson().encode(original.publication().orElseThrow()),new AirJson().encode(reordered.publication().orElseThrow())),"AIR deterministic under inventory order");metamorphics++;
        for(var name:List.of("bad-option","bad-length","missing-from","duplicate","truncated")) {
            var in=TerminalSendSuite.input(wire(name));need(control(in).stream().noneMatch(s->s.startsWith("NORMAL")),"unsupported SEND has no positive ordinary outcome");
        }
        var standalone=lower(wire("standalone"));bounded(standalone);var pub=standalone.publication().orElseThrow();var reached=TerminalSendSuite.reached(pub);
        need(pub.units().stream().flatMap(u->u.sequences().stream()).noneMatch(s->s.terminator() instanceof Operations.Invoke&&reached.contains(s.label())),"AIR must not execute post-SEND CALL");
        var bypass=lower(wire("abend-cancel")).admission().handlerState().orElseThrow().events().getFirst();need(bypass.status()==HandlerStateAnalysis.EventStatus.ASSESSED&&bypass.bypassed()&&bypass.candidates().isEmpty(),"ABEND CANCEL separate from local state");
        var unknown=lower(wire("cancel-entry")).admission().handlerState().orElseThrow().events().getFirst();need(unknown.candidates().isEmpty(),"unknown cannot invent dispatch");
        System.out.println("R7_R7B1_DIMENSION_CHECKS="+checks+" METAMORPHICS="+metamorphics);
    }
}
