       IDENTIFICATION DIVISION.
       PROGRAM-ID. SAMPLE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 RC PIC S9(8) COMP.
       PROCEDURE DIVISION.
       CALL 'KEEPPGM'.
       ALTER A TO PROCEED TO B.
       GO TO A.
       A.
       GO TO C.
       B.
       CALL 'ALTERPGM'.
       GOBACK.
       C.
       CALL 'TEXTUAL'.
       GOBACK.
