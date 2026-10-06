package io.github.gustavo2358.lower.adapters.sp;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.util.*;
/** Owned row admission; exact paragraph payload sharing, no semantic interpretation. */
final class SharedSpTree {
 private SharedSpTree() { }
 static JsonNode read(ObjectMapper mapper,Reader input)throws IOException {
  var elements=mapper.reader().without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  try(var parser=mapper.getFactory().createParser(input)) {
   parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);parser.nextToken();
   var result=object(parser,elements,mapper.getNodeFactory(),new HashMap<JsonNode,JsonNode>());
   if(parser.nextToken()!=null)throw new JsonParseException(parser,"Trailing content or second document");return result;
  }
 }
 private static JsonNode object(JsonParser p,ObjectReader reader,JsonNodeFactory factory,Map<JsonNode,JsonNode> paragraphs)throws IOException {
  if(p.currentToken()!=JsonToken.START_OBJECT)return reader.readTree(p);
  var result=factory.objectNode();
  while(p.nextToken()!=JsonToken.END_OBJECT) {
   if(p.currentToken()!=JsonToken.FIELD_NAME)throw new JsonParseException(p,"Object field required");
   String key=p.currentName();p.nextToken();JsonNode value;
   if((key.equals("statements")||key.equals("units"))&&p.currentToken()==JsonToken.START_ARRAY) {
    var rows=factory.arrayNode();
    while(p.nextToken()!=JsonToken.END_ARRAY) {
     if(p.currentToken()==null)throw new JsonParseException(p,"Truncated inventory");
     var row=key.equals("units")?object(p,reader,factory,paragraphs):reader.<JsonNode>readTree(p);
     if(key.equals("statements")&&row!=null&&row.isObject()&&row.path("procedures").isArray()) {
      var procedures=(ArrayNode)row.path("procedures");
      for(int i=0;i<procedures.size();i++){var original=procedures.get(i);if(readOnlyParagraph(original))procedures.set(i,paragraphs.computeIfAbsent(original,k->original));}
     }
     rows.add(row);
    }
    value=rows;
   } else if(key.equals("product")&&p.currentToken()==JsonToken.START_OBJECT)value=object(p,reader,factory,paragraphs);
   else value=reader.readTree(p);
   result.set(key,value);
  }
  return result;
 }
 private static boolean readOnlyParagraph(JsonNode node) {
  if(!node.isObject())return false;var keys=new HashSet<String>();node.fieldNames().forEachRemaining(keys::add);
  // Exact closed descriptor only; unknown/future payloads stay independently owned.
  return keys.equals(Set.of("id","entry","statements","completions","provenance"));
 }
}
