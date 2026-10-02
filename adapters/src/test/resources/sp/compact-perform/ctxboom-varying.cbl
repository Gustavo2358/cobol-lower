       IDENTIFICATION DIVISION.
       PROGRAM-ID. CTXBOOM.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-FLAG PIC X.
       01 WS-RETURN PIC 9(2).
       01 WS-I PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1
               UNTIL WS-I > 2
           GOBACK.
       RETURN-01.
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1
               UNTIL WS-I > 2
           GO TO DISPATCH.
       RETURN-02.
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1
               UNTIL WS-I > 2
           GO TO DISPATCH.
       BODY-START.
           IF WS-FLAG = 'Y'
               GO TO DISPATCH
           END-IF.
       BODY-END.
           EXIT.
       DISPATCH.
           GO TO RETURN-01 RETURN-02
                 DEPENDING ON WS-RETURN.
           GOBACK.
