       IDENTIFICATION DIVISION.
       PROGRAM-ID. PERFEVALUATE40.
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
               WHEN '10'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '11'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '12'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '13'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '14'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '15'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '16'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '17'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '18'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '19'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '20'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '21'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '22'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '23'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '24'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '25'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '26'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '27'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '28'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '29'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '30'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '31'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '32'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '33'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '34'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '35'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '36'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '37'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '38'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN '39'
                   MOVE 'PROGB001' TO WS-TARGET
           END-EVALUATE.
