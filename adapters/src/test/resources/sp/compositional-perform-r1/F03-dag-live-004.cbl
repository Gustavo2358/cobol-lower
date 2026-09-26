       IDENTIFICATION DIVISION.
       PROGRAM-ID. W6R1.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8).
       01 WS-DUMMY PIC X(8).
       01 WS-FLAG PIC X.
       01 WS-COUNT PIC 9(4).
       01 WS-RESP PIC S9(8) COMP.
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           PERFORM P0
           CALL WS-TARGET
           GOBACK.
       UNUSED.
           PERFORM P0.
       P0.
           PERFORM P1
           PERFORM P1.
       P1.
           PERFORM P2
           PERFORM P2.
       P2.
           PERFORM P3
           PERFORM P3.
       P3.
           PERFORM P4
           PERFORM P4.
       P4.
           MOVE 'PROGB001' TO WS-TARGET.
