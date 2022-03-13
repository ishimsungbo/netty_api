package com.netty;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Test {
    public static final long MINSIZE = 16384L;

    private static final Set<String> usingHeader = new HashSet<String>();

    static {
        usingHeader.add("token");
        usingHeader.add("email");
    }
    public static void main(String[] args) {
        System.out.println(MINSIZE);

        /**
         이터레이터 사용해보기
         */
        Iterator<String> itr = usingHeader.iterator();

        while (itr.hasNext()){
            System.out.println(itr.next());
        }
        System.out.println(usingHeader.contains("token1"));  //token 키가 포함되어 있는지 검사

        /**
        --
         */
    }
}
