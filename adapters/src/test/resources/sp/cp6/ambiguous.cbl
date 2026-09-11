       IDENTIFICATION DIVISION.
       PROGRAM-ID. CALLER.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 A.
         05 WS-PGM PIC X(8).
       01 B.
         05 WS-PGM PIC X(8).
       PROCEDURE DIVISION.
           CALL WS-PGM.
           GOBACK.
