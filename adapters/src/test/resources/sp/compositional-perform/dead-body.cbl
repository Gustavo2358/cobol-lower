       IDENTIFICATION DIVISION.
       PROGRAM-ID. W6PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8).
       01 WS-DUMMY PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           GO TO LIVE.
       DEAD.
           PERFORM P.
       LIVE.
           MOVE 'PROGA001' TO WS-TARGET
           CALL WS-TARGET
           GOBACK.
       P.
           CALL 'DEAD0001'.
