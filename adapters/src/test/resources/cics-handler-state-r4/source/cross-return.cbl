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
               PERFORM P
               EXEC CICS ABEND END-EXEC
           ELSE
           EXEC CICS HANDLE ABEND LABEL(B) END-EXEC
               PERFORM P
               EXEC CICS ABEND END-EXEC
           END-IF
           GOBACK.
       P.
           CONTINUE.
       A.
           GOBACK.
       B.
           GOBACK.
       C.
           GOBACK.
