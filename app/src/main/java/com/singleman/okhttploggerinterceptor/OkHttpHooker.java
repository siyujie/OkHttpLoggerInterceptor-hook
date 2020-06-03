package com.singleman.okhttploggerinterceptor;

import android.util.Log;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

import static com.singleman.okhttploggerinterceptor.OkCompat.Cls_OkHttpClient$Builder;
import static com.singleman.okhttploggerinterceptor.OkCompat.F_Builder_interceptors;
import static com.singleman.okhttploggerinterceptor.OkCompat.M_Builder_build;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;


/**
 * createTime: 2020/4/16.13:56
 * updateTime: 2020/4/16.13:56
 * author: singleMan.
 * desc:
 */
public class OkHttpHooker {

    public static final String TAG = "OkHttp";


    /**
     *
     * @param classLoader
     */
    public static void attach(final ClassLoader classLoader){

        Class OkHttpClient_BuilderClass = null;
        try {
            OkHttpClient_BuilderClass = Class.forName(Cls_OkHttpClient$Builder, true, classLoader);
            if(null == OkHttpClient_BuilderClass){
                return;
            }
            findAndHookMethod(OkHttpClient_BuilderClass, M_Builder_build, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    List interceptors = (List) getObjectField(param.thisObject, F_Builder_interceptors);
                    //添加自己的过滤器
                    interceptors.add(new MyInterceptor.Builder()
                            .addHeader("customHeaderKey","customHeaderValue")
                            .addQueryParam("customQueryKey","customQueryValue")
                            .build(classLoader));
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG,"没有发现OkHttp相关，可能未使用 or 被混淆");
        }


    }

}
