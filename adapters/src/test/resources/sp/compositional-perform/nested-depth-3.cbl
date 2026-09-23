       IDENTIFICATION DIVISION.
       PROGRAM-ID. W6PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8).
       01 WS-DUMMY PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           PERFORM P-1
           CALL WS-TARGET
           GOBACK.
       P-1.
           PERFORM P-2.
       P-2.
           PERFORM P-3.
       P-3.
           MOVE 'PROGB001' TO WS-TARGET.
