package com.netty.core;

import com.google.gson.JsonObject;
import com.netty.service.RequestParamException;
import com.netty.service.ServiceException;

/**
 *
 */
public interface ApiRequest {

    //api를 호출하는 http 요청의 파라미터 값이 입력되었는지 검증하는 메서드
    public void requestParamValidation() throws RequestParamException;

    //각 api 서비스에 따른 개별구현메서드
    public void service() throws ServiceException;


    public void executeService();

    //처리결과 조회
    public JsonObject getApiResult();
}
