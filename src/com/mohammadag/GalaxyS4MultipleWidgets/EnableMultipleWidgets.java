package com.mohammadag.GalaxyS4MultipleWidgets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class EnableMultipleWidgets implements IXposedHookLoadPackage {

	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("android"))
			return;
		
		Class<?> KeyguardHostView = XposedHelpers.findClass("com.android.internal.policy.impl.keyguard.KeyguardHostView", lpparam.classLoader);
		XposedBridge.hookAllConstructors(KeyguardHostView, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				setObjectField(param.thisObject, "mIsEasyUxOn", false);
			}
		});
		
		XposedHelpers.findAndHookMethod(KeyguardHostView, "addDefaultWidgets", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) {
				setObjectField(param.thisObject, "mIsEasyUxOn", false);
			}
		});
		
		XposedHelpers.findAndHookMethod(KeyguardHostView, "addWidgetsFromSettings", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) {
				setObjectField(param.thisObject, "mIsEasyUxOn", false);
			}
		});
		
		XposedHelpers.findAndHookMethod(KeyguardHostView, "shouldEnableAddWidget", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) {
				setObjectField(param.thisObject, "mIsEasyUxOn", false);
			}
		});
		
		XposedHelpers.findAndHookMethod(KeyguardHostView, "checkAppWidgetConsistency", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) {
				setObjectField(param.thisObject, "mIsEasyUxOn", false);
			}
		});
	}
	
}
