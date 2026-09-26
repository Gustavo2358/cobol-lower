       IDENTIFICATION DIVISION.
       PROGRAM-ID. PERFGOTOFANOUT2.
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
               DEPENDING ON WS-N
           GOBACK.
       P-0.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-1.
           MOVE 'PROGB001' TO WS-TARGET
           GO TO P-CALL.
       P-CALL.
           CALL WS-TARGET
           GOBACK.
