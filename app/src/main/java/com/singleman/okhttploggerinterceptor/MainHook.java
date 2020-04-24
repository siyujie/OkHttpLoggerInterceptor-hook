package com.singleman.okhttploggerinterceptor;

import android.text.TextUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * createTime: 2020/4/24.9:45
 * updateTime: 2020/4/24.9:45
 * author: singleMan.
 * desc:
 */
public class MainHook implements IXposedHookLoadPackage {

    final String TARGET_PACKAGE_NAME = "";//被Hook的包名

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if(TextUtils.equals(TARGET_PACKAGE_NAME,loadPackageParam.packageName)){
            //
            OkHttpHooker.attach(loadPackageParam.classLoader);

        }
    }
}
