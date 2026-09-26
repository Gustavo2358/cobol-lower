       identification division.
       program-id. SENDPROBE.
       data division.
       working-storage section.
       01 WS-DATA.
       05 CHILD PIC X(16).
       01 DEAD-AREA.
       05 DEAD-CHILD PIC X(8) OCCURS 20 TIMES.
       01 N PIC S9(4) COMP.
       01 CONV PIC X(4).
       procedure division.
           EXEC CICS HANDLE ABEND LABEL(ERR)
           END-EXEC.
           EXEC CICS SEND
               FROM(WS-DATA)
               LENGTH(LENGTH OF WS-DATA)
               NOHANDLE
               ERASE
           END-EXEC.
           EXEC CICS HANDLE ABEND CANCEL
           END-EXEC.
           EXEC CICS ABEND
           END-EXEC.
       ERR.
           GOBACK.
