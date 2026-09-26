       IDENTIFICATION DIVISION.
       PROGRAM-ID. STATECASE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 FLAG PIC 9.
       01 PGM PIC X(8).
       PROCEDURE DIVISION.
       MAIN-P.
           EXEC CICS HANDLE ABEND LABEL(A) END-EXEC
           PERFORM P 2 TIMES
           EXEC CICS ABEND END-EXEC.
       P.
           EXEC CICS HANDLE ABEND LABEL(B) END-EXEC
           CONTINUE.
       A.
           GOBACK.
       B.
           GOBACK.
       C.
           GOBACK.
