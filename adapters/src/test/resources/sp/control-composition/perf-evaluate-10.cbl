       IDENTIFICATION DIVISION.
       PROGRAM-ID. PERFEVALUATE10.
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
           EVALUATE WS-FLAG
               WHEN '0'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '1'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '2'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '3'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '4'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '5'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '6'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '7'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '8'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '9'
                   MOVE 'PROGB001' TO WS-TARGET
           END-EVALUATE.
