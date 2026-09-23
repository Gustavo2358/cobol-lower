       IDENTIFICATION DIVISION.
       PROGRAM-ID. PERFGOTOFANOUT10.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       01 WS-N PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           GO TO
               P-0
               P-1
               P-2
               P-3
               P-4
               P-5
               P-6
               P-7
               P-8
               P-9
               DEPENDING ON WS-N
           GOBACK.
       P-0.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-1.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-2.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-3.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-4.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-5.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-6.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-7.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-8.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-9.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-CALL.
           CALL WS-TARGET
           GOBACK.
