       IDENTIFICATION DIVISION.
       PROGRAM-ID. ORDINARYNUMERICDEADDEPENDING.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       01 WS-N PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           .
       P-CALL.
           CALL WS-TARGET
           GOBACK.
       P-DEAD.
           GO TO P-A P-B DEPENDING ON WS-N
           GOBACK.
       P-A.
           CONTINUE.
       P-B.
           GOBACK.
