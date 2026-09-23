       IDENTIFICATION DIVISION.
       PROGRAM-ID. TERMINALEVALUATEOTHERCONTINUE.
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
               WHEN 'Y'
                   MOVE 'PROGB001' TO WS-TARGET
               WHEN OTHER
                   CONTINUE
           END-EVALUATE.
