       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 G.
        05 ROW-X OCCURS 3 ASCENDING KEY KEY-X
            INDEXED BY IX.
         10 KEY-X PIC X.
       01 PGM PIC X(8).
       PROCEDURE DIVISION.
           SEARCH ALL ROW-X
           AT END GOBACK
           WHEN KEY-X(IX) = 'A'
             NEXT SENTENCE
           END-SEARCH
           CALL 'DEAD'.
           CALL 'AFTERDOT'
           GOBACK.
