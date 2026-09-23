       IDENTIFICATION DIVISION.
       PROGRAM-ID. GOBACKNOJOIN.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-TARGET PIC X(8).
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           IF WS-FLAG = 'Y'
               MOVE 'BADPGM01' TO WS-TARGET
               GOBACK
           ELSE
               MOVE 'PROGB001' TO WS-TARGET
           END-IF
           CALL WS-TARGET
           GOBACK.
