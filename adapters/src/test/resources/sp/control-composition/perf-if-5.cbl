       IDENTIFICATION DIVISION.
       PROGRAM-ID. PERFIF5.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           PERFORM P-WORK
           CALL WS-TARGET
           GOBACK.
       P-WORK.
           IF WS-FLAG = 'Y'
           IF WS-FLAG = 'Y'
           IF WS-FLAG = 'Y'
           IF WS-FLAG = 'Y'
           IF WS-FLAG = 'Y'
               MOVE 'PROGB001' TO WS-TARGET
           END-IF
           END-IF
           END-IF
           END-IF
           END-IF
           .
