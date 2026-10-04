# Integer DISPLAY MOVE — SP 2.65

Status: IN_PROGRESS. Consume the frontend numeric proof on each MOVE receiver. The frontend owns COBOL rules: IBM 6.4 elementary MOVE, unsigned unedited DISPLAY integers, fitting integer literal or integer DATA of no greater capacity. No physical encoding, sign removal, scale or truncation is inferred.

Admission must check typed source value, unique whole-item bindings, positive integer capacity, receiver identity, exact provenance and distinct transfer targets. DATA multi-target transfers cover a proved prefix, preserving the sending value after every certified write; an unsupported peer ends the prefix. Literal certificates may cover a subset; unsupported receivers retain their effect. Reject forged values, oversized literals, narrowed DATA, unrelated/duplicate targets and a numeric value labeled TEXT.

Build target indexes once: linear work in receivers and digit-string size, without powers, range enumeration or combinations. Lower admitted values to existing AIR INT Read/Literal and Assign. The proof goes through the ordinary MOVE dispatch and source identity digest. Validate in-memory and JSON, then AIR, CFG and dependency preservation on all 73 frozen CardDemo inputs.
