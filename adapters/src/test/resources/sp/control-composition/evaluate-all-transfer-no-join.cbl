       IDENTIFICATION DIVISION.
       PROGRAM-ID. EVALUATEALLTRANSFERNOJOIN.
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
                   GO TO P-C
           END-EVALUATE.
       P-A.
           MOVE 'PROGA001' TO WS-TARGET
           GO TO P-CALL.
       P-C.
           MOVE 'PROGC001' TO WS-TARGET
           GO TO P-CALL.
       P-CALL.
           CALL WS-TARGET
           GOBACK.
