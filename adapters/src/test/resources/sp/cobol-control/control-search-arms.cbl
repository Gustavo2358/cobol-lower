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
           MOVE 'BEFORE' TO PGM
           SEARCH ALL ROW-X
           AT END MOVE 'MISSPGM' TO PGM
           WHEN KEY-X(IX) = 'A'
             MOVE 'MATCHPGM' TO PGM
           END-SEARCH
           CALL PGM
           GOBACK.
