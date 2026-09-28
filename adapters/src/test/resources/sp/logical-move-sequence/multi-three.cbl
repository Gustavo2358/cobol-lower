       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROBE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 SRC PIC X(8).
       01 A PIC X(8).
       01 B PIC X(8).
       01 C PIC X(8).
       PROCEDURE DIVISION.
       MOVE 'PROGC001' TO SRC
       MOVE SRC TO A B C
       CALL A
       CALL B
       CALL C
       GOBACK.
