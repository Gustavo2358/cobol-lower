       IDENTIFICATION DIVISION.
       PROGRAM-ID. GOTOESCAPEPERFORMED.
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
               GO TO P-END
           ELSE
               MOVE 'PROGB001' TO WS-TARGET
           END-IF.
       P-END.
           CALL WS-TARGET
           GOBACK.
