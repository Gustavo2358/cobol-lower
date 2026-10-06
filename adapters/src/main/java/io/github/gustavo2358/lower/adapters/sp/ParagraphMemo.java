package io.github.gustavo2358.lower.adapters.sp;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
/** Identity selects a bucket only. Full immutable equality decides sharing; one owner, no payload hash. */
final class ParagraphMemo<K,P,V> {
 private record Entry<P,V>(P payload,V value,Entry<P,V> next) { }
 private final Map<K,Entry<P,V>> buckets=new HashMap<>();
 private final Function<P,K> identity;private final Function<P,V> translate;
 ParagraphMemo(Function<P,K> identity,Function<P,V> translate){this.identity=identity;this.translate=translate;}
 V get(P payload) {
  K key=identity.apply(payload);var first=buckets.get(key);
  for(var entry=first;entry!=null;entry=entry.next())if(entry.payload().equals(payload))return entry.value();
  V value=translate.apply(payload);buckets.put(key,new Entry<>(payload,value,first));return value;
 }
}
