package io.github.gustavo2358.lower.adapters.sp;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.*;
/** Full typed equality within one read; every physical check runs before memoization. */
final class ParagraphDtoSharing extends SimpleModule {
 private static final long serialVersionUID=1L;
 ParagraphDtoSharing() {
  setDeserializerModifier(new BeanDeserializerModifier() {
   @Override public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,BeanDescription bean,JsonDeserializer<?> delegate) {
    return bean.getBeanClass()==Wire211.PerformParagraphDocument.class?new Sharing(delegate):delegate;
   }
  });
 }
 private static final class Sharing extends DelegatingDeserializer {
  private static final long serialVersionUID=1L;
  Sharing(JsonDeserializer<?> delegate){super(delegate);}
  @Override protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> delegate){return new Sharing(delegate);}
  @Override public Object deserialize(JsonParser parser,DeserializationContext context)throws IOException {
   Object value=_delegatee.deserialize(parser,context);if(value==null)return null;
   Object attribute=context.getAttribute(ParagraphDtoSharing.class);
   Map<?,?> known;
   if(attribute==null){var fresh=new HashMap<Object,Object>();context.setAttribute(ParagraphDtoSharing.class,fresh);fresh.put(value,value);return value;}
   known=(Map<?,?>)attribute;Object previous=known.get(value);if(previous!=null)return previous;
   @SuppressWarnings("unchecked") var owned=(Map<Object,Object>)known;owned.put(value,value);return value;
  }
 }
}
