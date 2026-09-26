       identification division.
       program-id. SENDPROBE.
       data division.
       working-storage section.
       01 WS-DATA PIC X(16).
       01 N PIC S9(4) COMP.
       01 CONV PIC X(4).
       procedure division.
           GO TO LIVE.
       DEAD.
           EXEC CICS SEND
               FROM(WS-DATA)
               LENGTH(LENGTH OF WS-DATA)
               NOHANDLE ERASE
           END-EXEC.
       LIVE.
           GOBACK.
