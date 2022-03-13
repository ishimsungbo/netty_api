package com.netty;

import com.netty.core.ApiRequestParser;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.ssl.SslContext;

/**
http 프로토콜을 처리하기 위한 디코더와 인코더 그리고 api 서버의 로직 처리를 위한 ApiRequestPaser 가 포함되어 있다
 */
public class ApiServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslContext; // 1 ssl 를 사용한다면.. 아니면 null

    public ApiServerInitializer(SslContext sslContext) {
        this.sslContext = sslContext;
    }


    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline(); // 2 클라이언트로 수신된 http 데이터를 처리하기 위한 파이프라인 객체 생성
        if(sslContext != null){
            p.addLast(sslContext.newHandler(ch.alloc()));
        }

        p.addLast(new HttpRequestDecoder());  // 3 클라이언트가 전송한 http 프로토콜을 네티의 바이트 버퍼로 변환하는 작업을 수행한다.
        p.addLast(new HttpObjectAggregator(65536)); // 4 메시지 파편화를 처리하는 디코더 데이터를 하나로 합치고 65536 은 한꺼번에 처리 그 이상 수신되면 에러
        p.addLast(new HttpRequestEncoder()); // 5  클라이언트로 처리 결과를 넘길때 프로토콜로 변환해준다.
        p.addLast(new HttpContentCompressor()); // 6 gzip으로 압축, 해제 한다.
        p.addLast(new ApiRequestParser()); // 7 클라이언트로 수신된 http 헤더와 데이터 값을 추출하여 토큰발급과 같은 클래스로 분기하는 컨트롤러 역할 수행
    }
}
