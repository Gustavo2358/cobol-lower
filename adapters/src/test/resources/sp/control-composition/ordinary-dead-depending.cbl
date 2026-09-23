       IDENTIFICATION DIVISION.
       PROGRAM-ID. ORDINARYDEADDEPENDING.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           .
       P-CALL.
           CALL WS-TARGET
           GOBACK.
       P-DEAD.
           GO TO P-A P-B DEPENDING ON WS-FLAG
           GOBACK.
       P-A.
           CONTINUE.
       P-B.
           GOBACK.
