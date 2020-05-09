# OkHttpLoggerInterceptor-hook
Xposed实现的OkHttp日志拦截器，可添加Header &amp; Query参数

###  Log示例

```
 ┌───────────────────────────────────────────────────────
 │ --> POST http://******/user/detail?customQueryKey=customQueryValue http/1.1 (50-byte body)
 │ Content-Type: application/x-www-form-urlencoded
 │ Content-Length: 50
 │ customHeaderKey: customHeaderValue
 │ 
 │ userId=0&deviceId=8a746992663cb62d93e39b1eacc5cf10
 │ --> END POST (50-byte body)
 │ <-- 200  http://******/user/detail?customQueryKey=customQueryValue (278ms)
 │ Content-Type: application/json;charset=UTF-8
 │ Transfer-Encoding: chunked
 │ Date: Fri, 24 Apr 2020 02:21:42 GMT
 │ 
 │ {"code":1000,"message":"登录成功","result":{"userId":0,"username":"游客模式","password":null,"email":"mail.freevideo@mail.cn","presentAddress":"","avatar":"","sex":"保密","mobile":"","classes":0,"mustUrl":│"hSMGNEb3ZMMjlyYW5ndHA0TG1OdmJTOC9kWEpzUFE9Q=="}}
 │ <-- END HTTP (275-byte body)
 └────────────────────────────────────────────────────────
```
###  Build
```
new MyInterceptor.Builder()
                 .addHeader("customHeaderKey","customHeaderValue")
                 .addQueryParam("customQueryKey","customQueryValue")
                 .build(classLoader)
```


以上仅是简单实现，具体情况还得具体分析。
感谢珍惜大佬的XposedOkHttpCat
XposedOkHttpCat地址：https://github.com/w296488320/XposedOkHttpCat
