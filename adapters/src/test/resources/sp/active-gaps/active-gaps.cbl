       IDENTIFICATION DIVISION.
       PROGRAM-ID. ACTIVEGAPS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 TARGET-PGM PIC X(8).
       01 FLAG-X PIC X.
       01 N PIC 9.
       PROCEDURE DIVISION.
       MAIN.
           PERFORM UNTIL FLAG-X = 'Y'
               MOVE 'TARGET01' TO TARGET-PGM
               CONTINUE
               MOVE 'Y' TO FLAG-X
           END-PERFORM.
           PERFORM BODY-A.
           CALL TARGET-PGM.
           ADD 1 TO N.
           GOBACK.
       BODY-A.
           CONTINUE.
