       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 AREA-X PIC X(80).
       01 RESP-CD PIC S9(9) COMP.
       01 TRANS-ID PIC X(4).
       01 FLAG-X PIC X.
       PROCEDURE DIVISION.
           PERFORM 2 TIMES
             PERFORM 3 TIMES
               NEXT SENTENCE
               CALL 'DEAD1'
             END-PERFORM
             CALL 'DEAD2'
           END-PERFORM.
           CALL 'AFTERDOT'
           GOBACK.
