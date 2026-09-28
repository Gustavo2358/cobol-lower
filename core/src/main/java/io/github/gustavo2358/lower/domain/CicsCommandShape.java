package io.github.gustavo2358.lower.domain;
import java.util.*;
/** Closed typed option shape; no source parsing or control inference. */
final class CicsCommandShape {

    static boolean extended(SpInput.CicsCommandKind k) {return switch(k){case ASKTIME,FORMATTIME,ASSIGN,INQUIRE_PROGRAM,SEND_TEXT,WRITEQ_TD->true;default->false;};}
    static Set<String> extra(SpInput.CicsCommandKind k) {return switch(k) {
        case ASKTIME->Set.of("ABSTIME");
        case FORMATTIME->Set.of("ABSTIME","DATESEP","TIMESEP","TIME","MILLISECONDS","YYDDD","YYMMDD","MMDDYY","MMDDYYYY","YYYYMMDD");
        case ASSIGN->Set.of("APPLID","SYSID");case INQUIRE_PROGRAM->Set.of("PROGRAM");
        case SEND_TEXT->Set.of("TEXT","FROM","LENGTH","ERASE","FREEKB");case WRITEQ_TD->Set.of("TD","QUEUE","FROM","LENGTH","SYSID");default->Set.of();};}
    static boolean required(SpInput.CicsCommandKind k,Set<String> n) {return switch(k) {
        case ASKTIME,SYNCPOINT,RETURN->true;case FORMATTIME->n.contains("ABSTIME");case ASSIGN->n.contains("APPLID")||n.contains("SYSID");
        case INQUIRE_PROGRAM->n.contains("PROGRAM");case SEND_TEXT->n.containsAll(Set.of("TEXT","FROM"));case WRITEQ_TD->n.containsAll(Set.of("TD","QUEUE","FROM","LENGTH"));
        case SYNCPOINT_ROLLBACK->n.contains("ROLLBACK");case RETRIEVE->n.contains("INTO");case SEND_TERMINAL->n.contains("FROM");case RECEIVE_MAP,SEND_MAP->n.contains("MAP");};}
    static boolean flag(SpInput.CicsCommandKind k,String name,boolean operand) {return Set.of("NOHANDLE","CURSOR","ERASE","FREEKB","ROLLBACK","IMMEDIATE","TEXT","TD").contains(name)||k==SpInput.CicsCommandKind.FORMATTIME&&Set.of("DATESEP","TIMESEP").contains(name)&&!operand;}
    static boolean writes(SpInput.CicsCommandKind k,String name) {return Set.of("RESP","RESP2","INTO").contains(name)||k==SpInput.CicsCommandKind.ASKTIME&&name.equals("ABSTIME")||k==SpInput.CicsCommandKind.ASSIGN&&Set.of("APPLID","SYSID").contains(name)||k==SpInput.CicsCommandKind.FORMATTIME&&Set.of("TIME","MILLISECONDS","YYDDD","YYMMDD","MMDDYY","MMDDYYYY","YYYYMMDD").contains(name);}
    static boolean length(SpInput.CicsCommandKind k) {return k==SpInput.CicsCommandKind.SEND_TERMINAL||k==SpInput.CicsCommandKind.RETURN||k==SpInput.CicsCommandKind.SEND_TEXT||k==SpInput.CicsCommandKind.WRITEQ_TD;}
}
