       IDENTIFICATION DIVISION.
       PROGRAM-ID. ORDINARYCONTEXTUAL.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           PERFORM P-WORK
           CALL WS-TARGET
           MOVE 'PROGC001' TO WS-TARGET
           GO TO P-WORK.
       P-WORK.
           IF WS-FLAG = 'Y'
               MOVE 'PROGB001' TO WS-TARGET
           END-IF.
       P-END.
           CALL WS-TARGET
           GOBACK.
