package com.netty.core;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 프로토콜 처리
 * 클라이언트가 전송한 HTTP 프로토콜 데이터는 채널 파이프라인에 등록된 HTTP 프로토콜 코덱들을 거지촉 나면
 * FullHttpMessage 객체로 변환되어 마지막 데이터 핸들러인 ApiRequestParser 에 인바운드 이벤트로 전달된다.
 * FullHttpMessage 는 HttpMessage 와 HttpContent 인터페이스를 모두 상속한다.
 * HttpMessage 는 http 요청과 응답을 표현하는 인터페이스며 http 프로토콜 버전, 요청 url, http 헤더 정보 등이 포함된다.
 * HttpContent 클래스는 http 요청 프로토콜에 포함된 본문 데이터가 포함된다.
 * 클라이언트의 http 요청 데이터를 처리하는 ApiRequestParser => 크게 멤버 변수, 인바운드 핸들러 이벤트, http 본문 데이터를 읽는 부분으로 나뉜다.
 */
public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {

    private static final Logger logger = LogManager.getLogger(ApiRequestParser.class);

    private HttpRequest request;
    private JsonObject apiResult;

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk

    private HttpPostRequestDecoder decoder;

    private Map<String, String> reqData = new HashMap<String, String>();
    private static final Set<String> usingHeader = new HashSet<String>();

    static {
        usingHeader.add("token");
        usingHeader.add("email");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.info("요청 처리 완료");
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception { // 1
        // Request 헤더 처리
        if(msg instanceof HttpRequest){ // 2
            this.request = (HttpRequest) msg; // 3

            if(HttpHeaders.is100ContinueExpected(request)){
                send100Continue(ctx);
            }

            HttpHeaders headers = request.headers(); //4
            if(!headers.isEmpty()){
                for(Map.Entry<String,String> h : headers){
                    String key = h.getKey();
                    if(usingHeader.contains(key)){  //5
                        reqData.put(key, h.getValue());  //6
                    }
                }
            }
            reqData.put("REQUEST_URI",request.getUri()); //7
            reqData.put("REQUEST_METHOD",request.getMethod().name()); //8
        }

        // Request content 처리.
        if(msg instanceof HttpContent){ // 9
            HttpContent httpcontent = msg; // 10

            ByteBuf content = httpcontent.content();

            if(msg instanceof LastHttpContent){ // 12
                logger.debug("LastHttpContent message received!!" + request.getUri());

                LastHttpContent trailer = msg;

                readPostData(); // 13

                ApiRequest service = ServiceDispatcher.dispatch(reqData); //14 http 프로토콜에서 필요한 데이터의 추출이 완료되면 reqData 맵을
                                                                           //ServiceDispatcher 클래스의 dispatch 메서드를 호출해 요청에 맞는 api 서비스 클래스를 실행한다.

                try{
                    service.executeService(); //15
                    apiResult = service.getApiResult(); //16
                }finally {
                    reqData.clear();
                }
                if (!writeResponse(trailer, ctx)) {  // 17
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                reset();

            }
        }
    }
    private void reset() {
        request = null;
    }

    /**
     * http 본문 데이터를 수신하는 readPostData
     */
    private void readPostData() {
        try {
            decoder = new HttpPostRequestDecoder(factory, request);
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (InterfaceHttpData.HttpDataType.Attribute == data.getHttpDataType()) {
                    try {
                        Attribute attribute = (Attribute) data;
                        reqData.put(attribute.getName(), attribute.getValue());
                    }
                    catch (IOException e) {
                        logger.error("BODY Attribute: " + data.getHttpDataType().name(), e);
                        return;
                    }
                }
                else {
                    logger.info("BODY data : " + data.getHttpDataType().name() + ": " + data);
                }
            }
        }
        catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
            logger.error(e);
        }
        finally {
            if (decoder != null) {
                decoder.destroy();
            }
        }
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST, Unpooled.copiedBuffer(
                apiResult.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

        if (keepAlive) {
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
