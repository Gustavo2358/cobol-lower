       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 SRC PIC X(8) VALUE 'PROGA001'.
       01 A PIC X(8).
       01 B PIC X(8).
       PROCEDURE DIVISION.
           MOVE SRC TO A B
           CALL A
           CALL B
           GOBACK.
