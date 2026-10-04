       IDENTIFICATION DIVISION.
       PROGRAM-ID. CONDITION-CHECKPOINT.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8) VALUE 'OLD'.
          88 TARGET-A VALUES 'PROGA' 'ALT'.
          88 TARGET-B VALUE 'PROGB'
             WHEN SET TO FALSE IS 'NONE'.
       01 FLAG-RECORD.
          05 FLAG PIC X VALUE 'N'.
             88 READY VALUE 'Y'.
       01 COUNT-VALUE PIC 9(9) VALUE 7.
          88 IN-RANGE VALUE 7 20 THRU 999999999.
       PROCEDURE DIVISION.
           SET TARGET-A TARGET-B TO TRUE
           CALL WS-TARGET
           SET READY TO TRUE
           IF READY
               CALL 'READY'
           ELSE
               CALL 'NOTREADY'
           END-IF
           EVALUATE TRUE
               WHEN READY
                   CALL 'SELECTED'
               WHEN NOT READY
                   CALL 'UNSELECTED'
           END-EVALUATE
           PERFORM UNTIL READY
               CALL 'LOOP-BODY'
           END-PERFORM
           IF IN-RANGE CALL 'RANGE' END-IF
           SET TARGET-B TO FALSE
           CALL WS-TARGET
           GOBACK.
