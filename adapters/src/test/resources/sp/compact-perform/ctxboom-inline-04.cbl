       IDENTIFICATION DIVISION.
       PROGRAM-ID. INLINEBOOM.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-RETURN PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           GO TO RETURN-01.
       RETURN-01.
           PERFORM 1 TIMES
               IF WS-FLAG = 'Y'
                   GO TO DISPATCH
               END-IF
           END-PERFORM
           GO TO DISPATCH.
       RETURN-02.
           PERFORM 1 TIMES
               IF WS-FLAG = 'Y'
                   GO TO DISPATCH
               END-IF
           END-PERFORM
           GO TO DISPATCH.
       RETURN-03.
           PERFORM 1 TIMES
               IF WS-FLAG = 'Y'
                   GO TO DISPATCH
               END-IF
           END-PERFORM
           GO TO DISPATCH.
       RETURN-04.
           PERFORM 1 TIMES
               IF WS-FLAG = 'Y'
                   GO TO DISPATCH
               END-IF
           END-PERFORM
           GO TO DISPATCH.
       DISPATCH.
           GO TO RETURN-01 RETURN-02 RETURN-03 RETURN-04
                 DEPENDING ON WS-RETURN.
           GOBACK.
