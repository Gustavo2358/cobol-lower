       IDENTIFICATION DIVISION.
       PROGRAM-ID. STATECASE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 FLAG PIC 9.
       01 PGM PIC X(8).
       PROCEDURE DIVISION.
       MAIN-P.
           IF FLAG = 1
           EXEC CICS HANDLE ABEND LABEL(A) END-EXEC
           ELSE
           CONTINUE
           END-IF
           EXEC CICS ABEND END-EXEC.
       A.
           GOBACK.
       B.
           GOBACK.
       C.
           GOBACK.
