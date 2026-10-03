       IDENTIFICATION DIVISION.
       PROGRAM-ID. CTXBOOM.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-RETURN PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           PERFORM BODY-START THRU BODY-END
           GOBACK.
       RETURN-01.
           PERFORM BODY-START THRU BODY-END
           GO TO DISPATCH.
       RETURN-02.
           PERFORM BODY-START THRU BODY-END
           GO TO DISPATCH.
       BODY-START.
           PERFORM 2 TIMES
               PERFORM 3 TIMES
                   IF WS-FLAG = 'E'
                       EXIT PERFORM
                   END-IF
               END-PERFORM
           END-PERFORM
           IF WS-FLAG = 'Y'
               GO TO DISPATCH
           END-IF.
       BODY-END.
           EXIT.
       DISPATCH.
           GO TO RETURN-01 RETURN-02
                 DEPENDING ON WS-RETURN.
           GOBACK.
