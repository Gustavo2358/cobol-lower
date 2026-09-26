# Authoritative topology entry regression

Synthetic COBOL and unedited SP exported by frontend fbbf61d1840eb92dca804c53d8e9b6e60538318a. The missing period before LATER intentionally causes parser recovery. The legacy entry start is INPUT_MISSING, while the published topology root explicitly identifies the first CALL and gives it UNKNOWN_LOCAL continuation. Baseline lower 7bab3e9e8f7048afae235e8fcdb975840cfe1614 rejects ENTRY_START; the fix admits the current authority without creating a successor. KEEPPGM must survive; LATERPGM must not gain a path. The malformed input remains partial.
