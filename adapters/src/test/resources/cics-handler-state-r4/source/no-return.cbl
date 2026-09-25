       IDENTIFICATION DIVISION.
       PROGRAM-ID. STATECASE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 FLAG PIC 9.
       01 PGM PIC X(8).
       PROCEDURE DIVISION.
       MAIN-P.
           PERFORM P
           EXEC CICS ABEND END-EXEC.
       P.
           PERFORM P.
       A.
           GOBACK.
       B.
           GOBACK.
       C.
           GOBACK.
