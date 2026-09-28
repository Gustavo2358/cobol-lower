       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 AREA-X PIC X(80).
       01 RESP-CD PIC S9(9) COMP.
       01 TRANS-ID PIC X(4).
       01 FLAG-X PIC X.
       01 IN-X PIC X(8).
       01 PGM PIC X(8).
       01 SNAP PIC X(8).
       01 PCB-N PIC 9.
       01 OUT-X PIC X(8).
       EXEC SQL INCLUDE SQLCA END-EXEC.
       PROCEDURE DIVISION.
           MOVE 'FIRST' TO PGM
           PERFORM IO-P
           MOVE PGM TO SNAP
           MOVE 'SECOND' TO PGM
           CALL SNAP
           CALL PGM
           CALL 'AFTERP'
           GOBACK.
       IO-P.
           EXEC DLI CHKP ID(AREA-X) END-EXEC
           IF SQLCODE = 100 OR SQLSTATE = '02000'
               CALL 'NODATA'
           ELSE
               CALL 'OTHER'
           END-IF.
