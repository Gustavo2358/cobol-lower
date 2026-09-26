       IDENTIFICATION DIVISION.
       PROGRAM-ID. IFALLGOTO.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           IF WS-FLAG = 'A'
               GO TO P-A
           ELSE
               GO TO P-B
           END-IF.
           MOVE 'BADPGM01' TO WS-TARGET
           CALL WS-TARGET
           GOBACK.
       P-A.
           MOVE 'PROGA001' TO WS-TARGET
           GO TO P-CALL.
       P-B.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-CALL.
           CALL WS-TARGET
           GOBACK.
