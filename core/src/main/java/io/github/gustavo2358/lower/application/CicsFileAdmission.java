package io.github.gustavo2358.lower.application;
import io.github.gustavo2358.lower.domain.SpInput.*;
import java.util.*;
import static io.github.gustavo2358.lower.application.Admission.Rule;
/** Bilateral typed C-FC contract checks. Never reads or parses preserved COBOL text. */
final class CicsFileAdmission {
    private CicsFileAdmission() { }
    static final Set<String> READS=Set.of("READ","READNEXT","READPREV");
    static final Set<String> COMMANDS=Set.of("READ","WRITE","REWRITE","DELETE","STARTBR","READNEXT","READPREV","RESETBR","ENDBR","UNLOCK","INQUIRE","SET");
    static final Set<String> BROWSE=Set.of("STARTBR","READNEXT","READPREV","RESETBR","ENDBR");
    static void validate(CicsFileFact f,EntryGobackAdmission.Context c) {
        var h=f.header();var seen=new HashSet<OperandId>();
        require(c,f,COMMANDS.contains(f.command())&&f.nameProfile().equals("cics-ts.file@1"),"C-FC command and name profile");
        require(c,f,f.targetMode()==CicsFileTargetMode.INPUT||f.command().equals("INQUIRE")&&f.target().isEmpty(),"only INQUIRE output/browse has no input target");
        CallAdmission.continuation(f.ordinaryContinuation(),h,c);CallAdmission.continuation(f.localContinuation(),h,c);
        require(c,f,f.localContinuation().statement().isEmpty()||f.localContinuation().statement().equals(f.ordinaryContinuation().statement()),"local continuation agrees with ordinary continuation");
        var names=new HashSet<String>();f.options().forEach(o->names.add(o.canonicalName()));
        var expectedMode=!f.command().equals("INQUIRE")?CicsFileTargetMode.INPUT:names.contains("NEXT")?CicsFileTargetMode.OUTPUT:names.contains("START")?CicsFileTargetMode.BROWSE_START:names.contains("END")?CicsFileTargetMode.BROWSE_END:CicsFileTargetMode.INPUT;
        require(c,f,f.targetMode()==expectedMode,"target direction agrees with typed SPI mode");
        require(c,f,f.conditions()!=CicsConditions.DEFAULT_ENTRY_PREFIX,"FILE does not publish default handler premise");
        if(f.conditions()==CicsConditions.LOCAL_CONDITION)require(c,f,(names.contains("NOHANDLE")||names.contains("RESP"))&&wellFormed(f),"local condition requires coherent command/options and RESP or NOHANDLE");
        f.target().ifPresent(t->{if(t instanceof DataCallTarget d){CallAdmission.reference(d.reference(),h,seen,c);require(c,f,d.reference().role()==OperandRole.READ,"input FILE target must read its source");}else{var l=(LiteralCallTarget)t;CallAdmission.operand(l.id(),h,seen,c);c.provenance(l.provenance());l.logicalValue().ifPresent(v->{CallAdmission.logical(v,h,c);require(c,f,l.text().equals(v.value()),"FILE literal agrees with logical value");});}});
        for(var o:f.options()) {
            require(c,f,o.start()>=0&&o.end()>=o.start()&&o.end()<=f.rawText().length(),"option span inside preserved payload");
            var canonical=f.command().equals("SET")&&o.name().equals("DATASET")?"FILE":f.command().equals("SET")&&o.name().equals("OBJECTNAME")?"DSNAME":o.name();
            require(c,f,canonical.equals(o.canonicalName()),"aliases are command scoped");
            require(c,f,(o.reference().isPresent()?1:0)+(o.literal().isPresent()?1:0)+(o.integer().isPresent()?1:0)<=1,"one typed option value");
            require(c,f,o.role()==role(f,o.canonicalName(),names),"typed option role agrees with C-FC contract");
            o.reference().ifPresent(r->{CallAdmission.reference(r,h,seen,c);require(c,f,r.role()==(o.role()==CicsFileRole.WRITE?OperandRole.WRITE:OperandRole.READ),"host reference direction agrees");});
            if(o.canonicalName().equals("FILE")&&f.targetMode()==CicsFileTargetMode.INPUT){require(c,f,o.reference().isEmpty(),"input FILE host belongs to target");if(f.target().orElse(null) instanceof LiteralCallTarget l)require(c,f,o.literal().filter(l.text()::equals).isPresent(),"FILE literal agrees with target");}
        }
    }
    static boolean wellFormed(CicsFileFact f) {
        if(!Set.of("CICS_FILE_OUTCOME_VALUES_UNKNOWN","CICS_FILE_HOST_BINDING_UNAVAILABLE","CICS_FILE_TARGET_UNKNOWN").containsAll(f.gapCodes()))return false;
        var names=new HashSet<String>();
        for(var o:f.options()) {
            if(!names.add(o.canonicalName())||o.role()==CicsFileRole.UNKNOWN)return false;
            boolean bareFile=o.canonicalName().equals("FILE")&&(f.targetMode()==CicsFileTargetMode.BROWSE_START||f.targetMode()==CicsFileTargetMode.BROWSE_END);
            boolean flag=FLAGS.contains(o.canonicalName())&&!(o.canonicalName().equals("UPDATE")&&Set.of("INQUIRE","SET").contains(f.command()));
            if((flag||bareFile)==o.operand().isPresent())return false;
            if((o.role()==CicsFileRole.WRITE||o.role()==CicsFileRole.READ_WRITE)&&(o.literal().isPresent()||o.integer().isPresent()))return false;
        }
        if(!names.contains("FILE"))return false;
        if(READS.contains(f.command())&&(names.contains("INTO")==names.contains("SET")||!names.contains("RIDFLD")))return false;
        if(Set.of("WRITE","REWRITE").contains(f.command())&&!names.contains("FROM"))return false;
        if(Set.of("WRITE","STARTBR","RESETBR").contains(f.command())&&!names.contains("RIDFLD"))return false;
        for(var group:List.of(Set.of("START","NEXT","END"),Set.of("RBA","RRN","XRBA"),Set.of("GTEQ","EQUAL"),Set.of("UNCOMMITTED","CONSISTENT","REPEATABLE")))if(group.stream().filter(names::contains).count()>1)return false;
        return true;
    }
    private static void require(EntryGobackAdmission.Context c,CicsFileFact f,boolean value,String text){c.require(value,Rule.PROFILE_FACT,f.header().id().handle(),f.header().provenance(),text);}
    static CicsFileRole role(CicsFileFact f,String name,Set<String> names) {
        if(!Set.of("FILE","RESP","RESP2","NOHANDLE").contains(name)&&!CATALOG.getOrDefault(f.command(),Set.of()).contains(name))return CicsFileRole.UNKNOWN;
        if(name.equals("FILE")&&(f.targetMode()==CicsFileTargetMode.BROWSE_START||f.targetMode()==CicsFileTargetMode.BROWSE_END))return CicsFileRole.NONE;
        if(FLAGS.contains(name)&&!name.equals("UPDATE"))return CicsFileRole.NONE;
        if(name.equals("UPDATE")&&!Set.of("INQUIRE","SET").contains(f.command()))return CicsFileRole.NONE;
        if(name.equals("FILE"))return f.targetMode()==CicsFileTargetMode.OUTPUT?CicsFileRole.WRITE:CicsFileRole.READ;
        if(Set.of("RESP","RESP2").contains(name))return CicsFileRole.WRITE;
        if(f.command().equals("INQUIRE"))return CicsFileRole.WRITE;
        if(f.command().equals("SET"))return CicsFileRole.READ;
        if(Set.of("INTO","SET","NUMREC").contains(name))return CicsFileRole.WRITE;
        if(name.equals("TOKEN"))return READS.contains(f.command())?CicsFileRole.WRITE:CicsFileRole.READ;
        if(name.equals("LENGTH")&&READS.contains(f.command()))return CicsFileRole.READ_WRITE;
        if(name.equals("RIDFLD")){if(Set.of("READNEXT","READPREV").contains(f.command())||f.command().equals("READ")&&names.contains("GENERIC"))return CicsFileRole.READ_WRITE;if(f.command().equals("WRITE")&&(names.contains("RBA")||names.contains("XRBA")))return CicsFileRole.WRITE;}
        return CicsFileRole.READ;
    }
    private static final Set<String> FLAGS=words("NOHANDLE UNCOMMITTED CONSISTENT REPEATABLE UPDATE GENERIC EQUAL GTEQ DEBKEY DEBREC RBA RRN XRBA NOSUSPEND MASSINSERT START NEXT END ADDABLE NOTADDABLE BROWSABLE NOTBROWSABLE DELETABLE NOTDELETABLE OLD SHARE EMPTY EMPTYREQ NOEMPTYREQ DISABLED ENABLED CLOSED OPEN EXCTL NOEXCTL LOAD NOLOAD NOTREADABLE READABLE RLS NOTRLS CFTABLE CICSTABLE NOTTABLE USERTABLE NOTUPDATABLE UPDATABLE CONTENTION LOCKING WAIT FORCE NOWAIT");
    private static final Map<String,Set<String>> CATALOG=catalog();
    private static Set<String> words(String text){return Set.of(text.split(" "));}
    private static Map<String,Set<String>> catalog() {
        var m=new HashMap<String,Set<String>>();
        m.put("READ",words("INTO SET RIDFLD KEYLENGTH SYSID LENGTH TOKEN UNCOMMITTED CONSISTENT REPEATABLE UPDATE GENERIC EQUAL GTEQ DEBKEY DEBREC RBA RRN XRBA NOSUSPEND"));
        var browse=words("INTO SET RIDFLD KEYLENGTH SYSID LENGTH REQID TOKEN UNCOMMITTED CONSISTENT REPEATABLE UPDATE RBA RRN XRBA NOSUSPEND");
        m.put("READNEXT",browse);m.put("READPREV",browse);
        m.put("WRITE",words("FROM RIDFLD KEYLENGTH SYSID LENGTH MASSINSERT RBA RRN XRBA NOSUSPEND"));
        m.put("REWRITE",words("FROM LENGTH SYSID TOKEN NOSUSPEND"));
        m.put("DELETE",words("RIDFLD KEYLENGTH SYSID TOKEN GENERIC NOSUSPEND NUMREC"));
        m.put("STARTBR",words("RIDFLD KEYLENGTH SYSID REQID GENERIC DEBKEY DEBREC GTEQ EQUAL RBA RRN XRBA"));
        m.put("RESETBR",words("RIDFLD KEYLENGTH SYSID REQID GENERIC GTEQ EQUAL RBA RRN XRBA"));
        m.put("ENDBR",words("SYSID REQID"));m.put("UNLOCK",words("SYSID TOKEN"));
        m.put("INQUIRE",words("START NEXT END ACCESSMETHOD ADD BASEDSNAME BLOCKFORMAT BLOCKKEYLEN BLOCKSIZE BROWSE CFDTPOOL CHANGEAGENT CHANGEAGREL CHANGETIME CHANGEUSRID DEFINESOURCE DEFINETIME DELETE DISPOSITION DSNAME EMPTYSTATUS ENABLESTATUS EXCLUSIVE FWDRECSTATUS INSTALLAGENT INSTALLTIME INSTALLUSRID JOURNALNUM KEYLENGTH KEYPOSITION LOADTYPE LSRPOOLNUM MAXNUMRECS OBJECT OPENSTATUS RBATYPE READ READINTEG RECORDFORMAT RECORDSIZE RECOVSTATUS RELTYPE REMOTENAME REMOTESYSTEM REMOTETABLE RLSACCESS STRINGS TABLE TABLENAME TYPE UPDATE UPDATEMODEL"));
        m.put("SET",words("ADD ADDABLE NOTADDABLE BROWSE BROWSABLE NOTBROWSABLE BUSY WAIT FORCE NOWAIT CFDTPOOL DELETE DELETABLE NOTDELETABLE DISPOSITION OLD SHARE DSNAME EMPTYSTATUS EMPTY EMPTYREQ NOEMPTYREQ ENABLESTATUS DISABLED ENABLED OPENSTATUS CLOSED OPEN EXCLUSIVE EXCTL NOEXCTL KEYLENGTH LOADTYPE LOAD NOLOAD LSRPOOLNUM MAXNUMRECS READ NOTREADABLE READABLE RECORDSIZE READINTEG UNCOMMITTED CONSISTENT REPEATABLE RLSACCESS RLS NOTRLS STRINGS TABLE CFTABLE CICSTABLE NOTTABLE USERTABLE TABLENAME UPDATE NOTUPDATABLE UPDATABLE UPDATEMODEL CONTENTION LOCKING"));
        return Collections.unmodifiableMap(m);
    }
}
