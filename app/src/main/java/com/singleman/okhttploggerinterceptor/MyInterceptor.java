package com.singleman.okhttploggerinterceptor;

import android.util.Log;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.singleman.okhttploggerinterceptor.OkCompat.*;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.newInstance;


/**
 * createTime: 2020/3/27.18:09
 * updateTime: 2020/3/27.18:09
 * author: singleMan.
 * desc:  okhttp3 的过滤器
 */
public class MyInterceptor {

    private static final String TAG = OkHttpHooker.TAG;

    private static final String DEF_LINT_START = "│ ";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Builder builder;

    private MyInterceptor(Builder builder) {
        this.builder = builder;
    }

    private Object newInterceptor(final ClassLoader classLoader){
        Class<?> InterceptorClass = findClass(Cls_Interceptor, classLoader);
        Object myInterceptor = Proxy.newProxyInstance(classLoader, new Class[]{InterceptorClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if(M_Interceptor_intercept.equals(method.getName())){
                    synchronized (MyInterceptor.class){
                        try {
                            Class<?> BufferClass = findClass(Cls_okio_Buffer, classLoader);

                            Object chain = objects[0];

                            Object request = addQueryAndHeaders(callMethod(chain, M_chain_request));

                            Object requestBody = callMethod(request,M_req_body);

                            boolean hasRequestBody = requestBody != null;

                            Object connection = callMethod(chain, M_chain_connection);

                            Object protocol = connection != null ? callMethod(connection,M_connection_protocol) : "http/1.1";
//                            Object protocol =  "http/1.1";

                            Object httpUrl = callMethod(request,M_req_url);
                            String url = (String) callMethod(httpUrl,M_httpUrl_toString);
                            Log.d(TAG,"");
                            Log.d(TAG,"┌─────────────────────────────────────────────────────────────────────────────────────");
                            //
                            String requestStartMessage = "--> " + callMethod(request,M_req_method) + ' ' + callMethod(request,M_req_url) + ' ' + protocol;
                            if(hasRequestBody){
                                requestStartMessage += " (" + callMethod(requestBody,M_reqbody_contentLength) + "-byte body)";
                            }

                            Log.d(TAG,DEF_LINT_START+requestStartMessage);

                            if(hasRequestBody){
                                if (callMethod(requestBody,M_reqbody_contentType) != null) {
                                    Log.d(TAG,DEF_LINT_START+"Content-Type: " + callMethod(requestBody,M_reqbody_contentType));
                                }
                                if ((Long)callMethod(requestBody,M_reqbody_contentLength) != -1) {
                                    Log.d(TAG,DEF_LINT_START+"Content-Length: " + callMethod(requestBody,M_reqbody_contentLength));
                                }
                            }
                            Object headers = callMethod(request,M_req_headers);
                            int headersSize = (int) callMethod(headers,M_header_size);
                            for (int i = 0, count = headersSize; i < count; i++) {
                                String name = (String) callMethod(headers,M_header_name,i);
                                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                                    Log.d(TAG,DEF_LINT_START+name + ": " + callMethod(headers,M_header_value,i));
                                }
                            }
                            if(!hasRequestBody){
                                Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,M_req_method));
                            }else if(bodyEncoded(headers)){
                                Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,M_req_method) + " (encoded body omitted)");
                            }else {


                                Object buffer = newInstance(BufferClass);
                                callMethod(requestBody,M_reqbody_writeTo,buffer);
                                Charset charset = UTF8;

                                Object contentType = callMethod(requestBody,M_reqbody_contentType);
                                if (contentType != null) {
                                    charset = (Charset) callMethod(contentType,M_contentType_charset,UTF8);
                                }
                                Log.d(TAG,DEF_LINT_START+"");

                                if (isPlaintext(buffer,classLoader)) {
                                    Log.d(TAG, DEF_LINT_START+(String) callMethod(buffer,M_buffer_readString,charset));
                                    Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,M_req_method)
                                            + " (" + callMethod(requestBody,M_reqbody_contentLength) + "-byte body)");
                                } else {
                                    Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,M_req_method) + " (binary "
                                            + callMethod(requestBody,M_reqbody_contentLength) + "-byte body omitted)");
                                }
                            }

                            long startNs = System.nanoTime();
                            Object response;
                            try {
                                Method chain_proceed = chain.getClass().getMethod(M_chain_proceed, request.getClass());
                                response = chain_proceed.invoke(chain,request);
                            } catch (Throwable e) {
                                Log.d(TAG,DEF_LINT_START+"<-- HTTP FAILED: " + e);
                                throw e;
                            }
                            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

                            Object responseBody = callMethod(response,M_rsp_body);
                            long contentLength = (long) callMethod(responseBody,M_rspBody_contentLength);
                            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
                            Log.d(TAG,DEF_LINT_START+"<-- " + callMethod(response,M_rsp_code) + ' ' + callMethod(response,M_rsp_message) + ' '
                                    + callMethod(callMethod(response,M_rsp_request),M_req_url)
                                    + " (" + tookMs + "ms" + (!true ? ", "
                                    + bodySize + " body" : "") + ')');

                            Object resp_headers = callMethod(response,M_rsp_headers);
                            int respHeaderSize = (int) callMethod(resp_headers,M_header_size);
                            for (int i = 0, count = respHeaderSize; i < count; i++) {
                                Log.d(TAG,DEF_LINT_START+callMethod(resp_headers,M_header_name,i) + ": " + callMethod(resp_headers,M_header_value,i));
                            }

                            Class<?> HttpHeadersClass = findClass(Cls_HttpHeaders, classLoader);
                            if(!(boolean)callStaticMethod(HttpHeadersClass,M_HttpHeaders_hasBody,response)){
                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP");
                            }else if(bodyEncoded(resp_headers)){
                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP (encoded body omitted)");
                            }else {
                                Object source = callMethod(responseBody,M_rspBody_source);
                                callMethod(source,M_source_request,Long.MAX_VALUE);
                                Object resp_buffer = callMethod(source, M_source_buffer);

                                Charset charset = UTF8;

                                Object contentType = callMethod(responseBody,M_rspBody_contentType);
                                if (contentType != null) {
                                    try {
                                        charset = (Charset) callMethod(contentType,M_contentType_charset,UTF8);
                                    }catch (Exception e){
                                        Log.d(TAG,DEF_LINT_START+"");
                                        Log.d(TAG,DEF_LINT_START+"Couldn't decode the response body; charset is likely malformed.");
                                        Log.d(TAG,DEF_LINT_START+"<-- END HTTP");
                                        return response;
                                    }
                                }

                                if (!isPlaintext(resp_buffer,classLoader)) {
                                    Log.d(TAG,DEF_LINT_START+"");
                                    Log.d(TAG,DEF_LINT_START+"<-- END HTTP (binary " + callMethod(resp_buffer,M_buffer_size) + "-byte body omitted)");
                                    return response;
                                }
                                if (contentLength != 0) {
                                    Log.d(TAG,DEF_LINT_START+"");
                                    Log.d(TAG, DEF_LINT_START+(String) callMethod(callMethod(resp_buffer,M_buffer_clone),M_buffer_readString,charset));
                                }

                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP (" +  callMethod(resp_buffer,M_buffer_size) + "-byte body)");

                                Log.d(TAG,"└───────────────────────────────────────────────────────────────────────────────────────");
                                Log.d(TAG,"");
                            }

                            String content = (String) callMethod(responseBody, M_rspBody_string);

                            Object mediaType = callMethod(responseBody,M_rspBody_contentType);

                            Class<?> ok3ResponseBodyClass = findClass(Cls_ResponseBody, classLoader);

                            Object newBody = callStaticMethod(ok3ResponseBodyClass, M_rspBody_create, mediaType, content);

                            Object newBuilder = callMethod(response, M_rsp_newBuilder);

                            return callMethod(callMethod(newBuilder,M_rsp$builder_body,newBody),M_rsp$builder_build);


                        }catch (Exception e){
                            Log.d(TAG,Log.getStackTraceString(e));
                        }
                    }
                }
                return method.invoke(o,objects);
            }
        });
        return myInterceptor;
    }

    private boolean isPlaintext(Object buffer,ClassLoader classLoader) {
        try {
            Class<?> BufferClass = findClass(Cls_okio_Buffer, classLoader);
            Object prefix =newInstance(BufferClass);
            long size = (Long) callMethod(buffer,M_buffer_size);
            long byteCount = size < 64 ? size : 64;
            callMethod(buffer,M_buffer_copyTo,prefix, 0l, byteCount);
            for (int i = 0; i < 16; i++) {
                if ((boolean)callMethod(prefix,M_buffer_exhausted)) {
                    break;
                }
                int codePoint = (int) callMethod(prefix,M_buffer_readUtf8CodePoint);
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.d(TAG,Log.getStackTraceString(e));
            return false; // Truncated UTF-8 sequence.
        }
    }


    private  boolean bodyEncoded(Object headers) {
        String contentEncoding = (String) callMethod(headers,M_header_get,"Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }


    /**
     * 添加请求头
     * @param request
     * @return
     */
    private Object addQueryAndHeaders(Object request){
        Object requestBuilder = callMethod(request,M_req_newBuilder);
        Set<String> header_keySet = builder.headers.keySet();
        for(String key:header_keySet){
            callMethod(requestBuilder,M_Request$Builder_addHeader,key,builder.headers.get(key));//b
        }

        Object httpUrl = callMethod(request,M_req_url);

        String url = (String) callMethod(httpUrl,M_httpUrl_toString);

        Object httpUrlBuilder =  callMethod(httpUrl,M_httpUrl_newBuilder,url);

        Set<String> query_keySet = builder.queryMap.keySet();
        for(String key:query_keySet){
            callMethod(httpUrlBuilder,M_httpUrl_addQueryParameter,key,builder.queryMap.get(key));
        }
        return callMethod(callMethod(requestBuilder,M_Request$Builder_url,callMethod(httpUrlBuilder,M_httpUrl_build)),M_Request$Builder_build);
    }


    static class Builder {
        private Map<String,String> headers;
        private Map<String,String> queryMap;

        public Builder() {
            this.headers = new HashMap<>();
            this.queryMap = new HashMap<>();
        }

        public Builder addHeader(String key,String value){
            headers.put(key,value);
            return this;
        }
        public Builder addQueryParam(String key,String value){
            queryMap.put(key,value);
            return this;
        }

        /**
         * @param classLoader
         * @return  Interceptor
         */
        public Object build(ClassLoader classLoader){
            return new MyInterceptor(this).newInterceptor(classLoader);
        }
    }


}
