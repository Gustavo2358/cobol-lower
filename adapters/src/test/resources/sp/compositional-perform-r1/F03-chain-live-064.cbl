       IDENTIFICATION DIVISION.
       PROGRAM-ID. W6R1.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TARGET PIC X(8).
       01 WS-DUMMY PIC X(8).
       01 WS-FLAG PIC X.
       01 WS-COUNT PIC 9(4).
       01 WS-RESP PIC S9(8) COMP.
       PROCEDURE DIVISION.
       MAIN.
           MOVE 'PROGA001' TO WS-TARGET
           PERFORM P0
           CALL WS-TARGET
           GOBACK.
       UNUSED.
           PERFORM P0.
       P0.
           PERFORM P1.
       P1.
           PERFORM P2.
       P2.
           PERFORM P3.
       P3.
           PERFORM P4.
       P4.
           PERFORM P5.
       P5.
           PERFORM P6.
       P6.
           PERFORM P7.
       P7.
           PERFORM P8.
       P8.
           PERFORM P9.
       P9.
           PERFORM P10.
       P10.
           PERFORM P11.
       P11.
           PERFORM P12.
       P12.
           PERFORM P13.
       P13.
           PERFORM P14.
       P14.
           PERFORM P15.
       P15.
           PERFORM P16.
       P16.
           PERFORM P17.
       P17.
           PERFORM P18.
       P18.
           PERFORM P19.
       P19.
           PERFORM P20.
       P20.
           PERFORM P21.
       P21.
           PERFORM P22.
       P22.
           PERFORM P23.
       P23.
           PERFORM P24.
       P24.
           PERFORM P25.
       P25.
           PERFORM P26.
       P26.
           PERFORM P27.
       P27.
           PERFORM P28.
       P28.
           PERFORM P29.
       P29.
           PERFORM P30.
       P30.
           PERFORM P31.
       P31.
           PERFORM P32.
       P32.
           PERFORM P33.
       P33.
           PERFORM P34.
       P34.
           PERFORM P35.
       P35.
           PERFORM P36.
       P36.
           PERFORM P37.
       P37.
           PERFORM P38.
       P38.
           PERFORM P39.
       P39.
           PERFORM P40.
       P40.
           PERFORM P41.
       P41.
           PERFORM P42.
       P42.
           PERFORM P43.
       P43.
           PERFORM P44.
       P44.
           PERFORM P45.
       P45.
           PERFORM P46.
       P46.
           PERFORM P47.
       P47.
           PERFORM P48.
       P48.
           PERFORM P49.
       P49.
           PERFORM P50.
       P50.
           PERFORM P51.
       P51.
           PERFORM P52.
       P52.
           PERFORM P53.
       P53.
           PERFORM P54.
       P54.
           PERFORM P55.
       P55.
           PERFORM P56.
       P56.
           PERFORM P57.
       P57.
           PERFORM P58.
       P58.
           PERFORM P59.
       P59.
           PERFORM P60.
       P60.
           PERFORM P61.
       P61.
           PERFORM P62.
       P62.
           PERFORM P63.
       P63.
           PERFORM P64.
       P64.
           MOVE 'PROGB001' TO WS-TARGET.
