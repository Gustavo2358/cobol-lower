       IDENTIFICATION DIVISION.
       PROGRAM-ID. GAPVERSIONS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 FLAG-X PIC X.
       PROCEDURE DIVISION.
       MAIN.
           CONTINUE.
           IF FLAG-X = 'X'
               PERFORM BODY-A
           END-IF.
           PERFORM UNTIL FLAG-X = 'Y'
               MOVE 'Y' TO FLAG-X
               CONTINUE
           END-PERFORM.
           GOBACK.
       BODY-A.
           MOVE 'Y' TO FLAG-X.
       CONDITION-SETUP.
           EXEC CICS IGNORE CONDITION ERROR END-EXEC.
