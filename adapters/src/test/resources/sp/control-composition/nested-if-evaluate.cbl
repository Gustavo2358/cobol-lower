       IDENTIFICATION DIVISION.
       PROGRAM-ID. NESTEDIFEVALUATE.
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
               EVALUATE WS-FLAG
                   WHEN 'A'
                       MOVE 'PROGB001' TO WS-TARGET
                   WHEN OTHER
                       IF WS-FLAG = 'Z'
                           MOVE 'PROGC001' TO WS-TARGET
                       END-IF
               END-EVALUATE
           ELSE
               CONTINUE
           END-IF.
