package io.github.gustavo2358.lower.adapters.sp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Historical wire certificates are validated once; all versions enter the same symbolic fitting model. */
final class TextFittingWire {
    private TextFittingWire() { }
    static boolean normalize(JsonNode root,String version) {
        for(var statement:root.path("statements")) {
            var adjustment=statement.path("textAdjustment");if(!adjustment.isObject())continue;
            if(version.equals("2.66.0")) {if(adjustment.has("result"))return false;continue;}
            var result=adjustment.path("result");var source=statement.path("source").path("logicalValue");
            if(!result.path("value").isTextual()||!source.path("value").isTextual()
                    ||!result.path("logicalDomain").asText().equals("TEXT")||!adjustment.path("receiverExtent").canConvertToInt())return false;
            int extent=adjustment.path("receiverExtent").intValue();String value=result.path("value").textValue(),from=source.path("value").textValue();
            if(extent<=0||value.codePointCount(0,value.length())!=extent||result.path("logicalExtent").asInt(-1)!=extent)return false;
            // Visit only the already supplied certificate. Never allocate padding by receiver extent.
            int offset=0;
            for(int at=0;at<value.length();) {
                int actual=value.codePointAt(at);at+=Character.charCount(actual);
                int expected=offset<from.length()?from.codePointAt(offset):' ';
                if(offset<from.length())offset+=Character.charCount(expected);
                if(actual!=expected)return false;
            }
            ((ObjectNode)adjustment).remove("result");
        }
        return true;
    }
}
