       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 AREA-X PIC X(80).
       01 RESP-CD PIC S9(9) COMP.
       01 TRANS-ID PIC X(4).
       01 FLAG-X PIC X.
       PROCEDURE DIVISION.
           PERFORM IO-P
           CALL 'AFTERIO'
           GOBACK.
       IO-P.
           EXEC CICS STARTBR FILE('DDNAME')
                RIDFLD(FLAG-X)
                RESP(RESP-CD) END-EXEC.
