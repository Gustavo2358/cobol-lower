       IDENTIFICATION DIVISION.
       PROGRAM-ID. CTXBOOM.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-I PIC 9(2).
       01 WS-FLAG PIC X.
       01 WS-RETURN PIC 9(2).
       PROCEDURE DIVISION.
       MAIN.
           EXEC CICS HANDLE ABEND LABEL(ERR) END-EXEC
           EXEC CICS LINK PROGRAM('BEFORE') END-EXEC
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2
           EXEC CICS LINK PROGRAM('AFTER') END-EXEC
           GOBACK.
       RETURN-01.
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2
           GO TO DISPATCH.
       RETURN-02.
           PERFORM BODY-START THRU BODY-END
               VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2
           GO TO DISPATCH.
       BODY-START.
           PERFORM 2 TIMES
               PERFORM 3 TIMES
                   IF WS-FLAG = 'E'
                       EXIT PARAGRAPH
                   END-IF
               END-PERFORM
           END-PERFORM
           EXEC CICS HANDLE ABEND LABEL(NEW-ERR) END-EXEC
           IF WS-FLAG = 'Y'
               GO TO DISPATCH
           END-IF.
       BODY-END.
           EXIT.
       DISPATCH.
           GO TO RETURN-01 RETURN-02
                 DEPENDING ON WS-RETURN.
           GOBACK.
       ERR.
           CALL 'OLDHDLR'
           GOBACK.
       NEW-ERR.
           CALL 'NEWHDLR'
           GOBACK.
