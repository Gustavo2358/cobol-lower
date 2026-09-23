       IDENTIFICATION DIVISION.
       PROGRAM-ID. T0000039.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8).
       01 WS-SECOND PIC X(8).
       01 WS-FLAG PIC X.
       01 WS-DONE PIC X.
       01 WS-I PIC 9(4).
       01 WS-J PIC 9(4).
       01 WS-N PIC 9(4).
       01 WS-DUMMY PIC X.
       01 WS-DEAD PIC X(8) VALUE 'DEAD0001'.
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           GO TO P-DONE.
       P-DEAD.
           MOVE 'DEAD0001' TO WS-TARGET
           CALL WS-TARGET.
       P-DONE.
           CALL WS-TARGET
           GOBACK.
