       IDENTIFICATION DIVISION.
       PROGRAM-ID. EVALUATEMIXEDEXITS.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           EVALUATE WS-FLAG
               WHEN 'A'
                   GO TO P-A
               WHEN 'B'
                   GOBACK
               WHEN OTHER
                   PERFORM P-Y
           END-EVALUATE
           CALL WS-TARGET
           GOBACK.
       P-A.
           MOVE 'PROGA001' TO WS-TARGET
           CALL WS-TARGET
           GOBACK.
       P-Y.
           MOVE 'PROGC001' TO WS-TARGET
           CONTINUE.
