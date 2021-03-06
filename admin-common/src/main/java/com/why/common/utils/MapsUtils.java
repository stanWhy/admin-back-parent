package com.why.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @description: Map工具集 ｜ http://minborgsjavapot.blogspot.com/2014/12/java-8-initializing-maps-in-smartest-way.html
 * @author: Stan | stan.wang@paytm.com
 * @create: 2021/1/22 8:29 下午
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapsUtils {
    
    public static <K,V> MapBuilder<K,V> builder(){
        return builder(HashMap::new);
    }
    
    public static <K,V> MapBuilder<K,V> builder(Supplier<Map<K,V>> mapSupplier){
        return new MapBuilder<>(mapSupplier.get());
    }

    public static <K,V> ConcurrentMapBuilder<K,V> concurrentBuilder(){
        return concurrentBuilder(ConcurrentHashMap::new);
    }

    public static <K, V> ConcurrentMapBuilder<K, V> concurrentBuilder(Supplier<ConcurrentMap<K, V>> mapSupplier) {
        return new ConcurrentMapBuilder<>(mapSupplier.get());
    }
    
    public static class BaseBuilder<M extends Map<K, V>, K, V> {
        protected final M map;
        public BaseBuilder(M map){
            this.map = map;
        }
        public BaseBuilder<M, K, V> put(K k, V v){
            map.put(k,v);
            return this;
        }
        public M build(){
            return map;
        }
    }
    public static class MapBuilder<K, V> extends BaseBuilder<Map<K, V>, K, V> {
        
        private boolean unmodifiable;

        public MapBuilder(Map<K, V> map) {
            super(map);
        }

        @Override
        public MapBuilder<K,V> put(K k, V v){
            super.put(k, v);
            return this;
        }
        
        public MapBuilder<K, V> unmodifiable(boolean unmodifiable){
            this.unmodifiable = unmodifiable;
            return this;
        }
        
        public MapBuilder<K, V> unmodifiable(){
            return unmodifiable(true);
        }
        
        @Override
        public Map<K,V> build(){
            if(unmodifiable){
                return Collections.unmodifiableMap(super.build());
            }else{
                return super.build();
            }
        }
    }
    
    public static class ConcurrentMapBuilder<K, V> extends BaseBuilder<ConcurrentMap<K,V>,K,V>{
        public ConcurrentMapBuilder(ConcurrentMap<K,V> map){
            super(map);
        }
        @Override
        public ConcurrentMapBuilder<K,V> put(K k,V v){
            super.put(k,v);
            return this;
        }
    }
}
