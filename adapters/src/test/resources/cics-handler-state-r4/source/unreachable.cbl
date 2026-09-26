       IDENTIFICATION DIVISION.
       PROGRAM-ID. STATECASE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 FLAG PIC 9.
       01 PGM PIC X(8).
       PROCEDURE DIVISION.
       MAIN-P.
           GO TO AFTER-P.
       DEAD-P.
           EXEC CICS HANDLE ABEND LABEL(B) END-EXEC
           CONTINUE.
       AFTER-P.
           EXEC CICS ABEND END-EXEC.
       A.
           GOBACK.
       B.
           GOBACK.
       C.
           GOBACK.
