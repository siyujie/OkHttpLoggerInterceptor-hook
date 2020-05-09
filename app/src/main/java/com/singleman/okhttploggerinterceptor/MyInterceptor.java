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
 * 逻辑是从 com.squareup.okhttp3:logging-interceptor 中拿来的
 * 备注：目前只支持没有混淆的OkHttp，如果混淆后，需要自行替换被混淆后的hook点
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
        Class<?> InterceptorClass = findClass("okhttp3.Interceptor", classLoader);
        Object myInterceptor = Proxy.newProxyInstance(classLoader, new Class[]{InterceptorClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if("intercept".equals(method.getName())){
                    synchronized (MyInterceptor.class){
                        Log.d(TAG,"");
                        Log.d(TAG,"┌─────────────────────────────────────────────────────────────────────────────────────");
                        try {
                            Class<?> BufferClass = findClass("okio.Buffer", classLoader);

                            Object chain = objects[0];

                            Object request = addQueryAndHeaders(callMethod(chain, "request"));

                            Object requestBody = callMethod(request,"body");

                            boolean hasRequestBody = requestBody != null;

                            Object connection = callMethod(chain, "connection");

                            Object protocol = connection != null ? callMethod(connection,"protocol") : "http/1.1";

                            String requestStartMessage = "--> " + callMethod(request,"method") + ' ' + callMethod(request,"url") + ' ' + protocol;
                            if(hasRequestBody){
                                requestStartMessage += " (" + callMethod(requestBody,"contentLength") + "-byte body)";
                            }

                            Log.d(TAG,DEF_LINT_START+requestStartMessage);

                            if(hasRequestBody){
                                if (callMethod(requestBody,"contentType") != null) {
                                    Log.d(TAG,DEF_LINT_START+"Content-Type: " + callMethod(requestBody,"contentType"));
                                }
                                if ((Long)callMethod(requestBody,"contentLength") != -1) {
                                    Log.d(TAG,DEF_LINT_START+"Content-Length: " + callMethod(requestBody,"contentLength"));
                                }
                            }
                            Object headers = callMethod(request,"headers");
                            int headersSize = (int) callMethod(headers,"size");
                            for (int i = 0, count = headersSize; i < count; i++) {
                                String name = (String) callMethod(headers,"name",i);
                                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                                    Log.d(TAG,DEF_LINT_START+name + ": " + callMethod(headers,"value",i));
                                }
                            }
                            if(!hasRequestBody){
                                Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,"method"));
                            }else if(bodyEncoded(headers)){
                                Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,"method") + " (encoded body omitted)");
                            }else {


                                Object buffer = newInstance(BufferClass);
                                callMethod(requestBody,"writeTo",buffer);
                                Charset charset = UTF8;

                                Object contentType = callMethod(requestBody,"contentType");
                                if (contentType != null) {
                                    charset = (Charset) callMethod(contentType,"charset",UTF8);
                                }
                                Log.d(TAG,DEF_LINT_START+"");

                                if (isPlaintext(buffer,classLoader)) {
                                    Log.d(TAG, DEF_LINT_START+(String) callMethod(buffer,"readString",charset));
                                    Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,"method")
                                            + " (" + callMethod(requestBody,"contentLength") + "-byte body)");
                                } else {
                                    Log.d(TAG,DEF_LINT_START+"--> END " + callMethod(request,"method") + " (binary "
                                            + callMethod(requestBody,"contentLength") + "-byte body omitted)");
                                }
                            }

                            long startNs = System.nanoTime();
                            Object response;
                            try {
                                Method proceed = findMethodExact("okhttp3.Interceptor.Chain",classLoader, "proceed", findClass("okhttp3.Request",classLoader));
                                response = proceed.invoke(chain,request);
//                                response = callMethod(chain,"proceed",request);
                            } catch (Exception e) {
                                Log.d(TAG,DEF_LINT_START+"<-- HTTP FAILED: " + e);
                                throw e;
                            }
                            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

                            Object responseBody = callMethod(response,"body");
                            long contentLength = (long) callMethod(responseBody,"contentLength");
                            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
                            Log.d(TAG,DEF_LINT_START+"<-- " + callMethod(response,"code") + ' ' + callMethod(response,"message") + ' '
                                    + callMethod(callMethod(response,"request"),"url")
                                    + " (" + tookMs + "ms" + (!true ? ", "
                                    + bodySize + " body" : "") + ')');

                            Object resp_headers = callMethod(response,"headers");
                            int respHeaderSize = (int) callMethod(resp_headers,"size");
                            for (int i = 0, count = respHeaderSize; i < count; i++) {
                                Log.d(TAG,DEF_LINT_START+callMethod(resp_headers,"name",i) + ": " + callMethod(resp_headers,"value",i));
                            }

                            Class<?> HttpHeadersClass = findClass("okhttp3.internal.http.HttpHeaders", classLoader);
                            if(!(boolean)callStaticMethod(HttpHeadersClass,"hasBody",response)){
                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP");
                            }else if(bodyEncoded(resp_headers)){
                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP (encoded body omitted)");
                            }else {
                                Object source = callMethod(responseBody,"source");
                                callMethod(source,"request",Long.MAX_VALUE);
                                Object resp_buffer = callMethod(source, "buffer");

                                Charset charset = UTF8;

                                Object contentType = callMethod(responseBody,"contentType");
                                if (contentType != null) {
                                    try {
                                        charset = (Charset) callMethod(contentType,"charset",UTF8);
                                    }catch (Exception e){
                                        Log.d(TAG,DEF_LINT_START+"");
                                        Log.d(TAG,DEF_LINT_START+"Couldn't decode the response body; charset is likely malformed.");
                                        Log.d(TAG,DEF_LINT_START+"<-- END HTTP");
                                        return response;
                                    }
                                }

                                if (!isPlaintext(resp_buffer,classLoader)) {
                                    Log.d(TAG,DEF_LINT_START+"");
                                    Log.d(TAG,DEF_LINT_START+"<-- END HTTP (binary " + callMethod(resp_buffer,"size") + "-byte body omitted)");
                                    return response;
                                }
                                if (contentLength != 0) {
                                    Log.d(TAG,DEF_LINT_START+"");
                                    Log.d(TAG, DEF_LINT_START+(String) callMethod(callMethod(resp_buffer,"clone"),"readString",charset));
                                }

                                Log.d(TAG,DEF_LINT_START+"<-- END HTTP (" +  callMethod(resp_buffer,"size") + "-byte body)");

                                Log.d(TAG,"└───────────────────────────────────────────────────────────────────────────────────────");
                                Log.d(TAG,"");
                            }

                            String content = (String) callMethod(responseBody, "string");

                            Object mediaType = callMethod(responseBody,"contentType");

                            Class<?> ok3ResponseBodyClass = findClass("okhttp3.ResponseBody", classLoader);

                            Object newBody = callStaticMethod(ok3ResponseBodyClass, "create", mediaType, content);

                            Object newBuilder = callMethod(response, "newBuilder");

                            return callMethod(callMethod(newBuilder,"body",newBody),"build");


                        }catch (Exception e){
//                            Log.d(TAG,Log.getStackTraceString(e));
                            Log.d(TAG,"└───────────ERROR────────────────────────────────────────────────────────────────────────────");
                            Log.d(TAG,"");
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
            Class<?> BufferClass = findClass("okio.Buffer", classLoader);
            Object prefix =newInstance(BufferClass);
            long size = (Long) callMethod(buffer,"size");
            long byteCount = size < 64 ? size : 64;
            callMethod(buffer,"copyTo",prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if ((boolean)callMethod(prefix,"exhausted")) {
                    break;
                }
                int codePoint = (int) callMethod(prefix,"readUtf8CodePoint");
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
        String contentEncoding = (String) callMethod(headers,"get","Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }


    /**
     * 添加自定义query & header
     * @param request
     * @return
     */
    private Object addQueryAndHeaders(Object request){
        Object requestBuilder = callMethod(request,"newBuilder");
        Set<String> header_keySet = builder.headers.keySet();
        for(String key:header_keySet){
            callMethod(requestBuilder,"addHeader",key,builder.headers.get(key));
        }

        Object httpUrl = callMethod(request,"url");

        String url = (String) callMethod(httpUrl,"toString");

        Object httpUrlBuilder =  callMethod(httpUrl,"newBuilder",url);

        Set<String> query_keySet = builder.queryMap.keySet();
        for(String key:query_keySet){
            callMethod(httpUrlBuilder,"addQueryParameter",key,builder.queryMap.get(key));
        }
        return callMethod(callMethod(requestBuilder,"url",callMethod(httpUrlBuilder,"build")),"build");
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
