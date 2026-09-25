           EXEC CICS SEND
               FROM(WS-DATA)
               LENGTH(LENGTH OF WS-DATA)
               NOHANDLE ERASE
           END-EXEC.
