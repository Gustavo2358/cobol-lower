       identification division.
       program-id. SENDPROBE.
       data division.
       working-storage section.
       01 WS-DATA PIC X(16).
       01 N PIC S9(4) COMP.
       01 CONV PIC X(4).
       procedure division.
           EXEC CICS HANDLE ABEND LABEL(ERR)
           END-EXEC.
           EXEC CICS SEND
               ERASE
           END-EXEC.
           EXEC CICS HANDLE ABEND CANCEL
           END-EXEC.
           EXEC CICS ABEND
           END-EXEC.
       ERR.
           GOBACK.
