package com.mohammadag.multiplewidgets;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import java.lang.reflect.Method;

import android.preference.SwitchPreference;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class EnableMultipleWidgets implements IXposedHookLoadPackage {

	//private XSharedPreferences prefs;
	
	/*
	 * Can't figure this out, screw it, modularize all mods!
	@SuppressLint("SdCardPath")
	public void initZygote(StartupParam param) throws Throwable {
		File file = new File(EnableMultipleWidgets.class.getPackage().getName());
		prefs = new XSharedPreferences(file);
	}
	*/
	
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		// Allow the user to switch between secure camera or Samsung's shortcuts.
		if (lpparam.packageName.equals("com.android.settings")) {
			XposedHelpers.findAndHookMethod("com.android.settings.LockScreenSettings", lpparam.classLoader, "onResume", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) {
					SwitchPreference mShortCameraWidget = (SwitchPreference) getObjectField(param.thisObject, "mShortCameraWidget");
					mShortCameraWidget.setEnabled(true);
				}
			});
			return;
		}
		
		if (!lpparam.packageName.equals("android"))
			return;
		
		final Class<?> KeyguardHostView = findClass("com.android.internal.policy.impl.keyguard.KeyguardHostView", lpparam.classLoader);
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
		
		// Allow reordering of lock screen widgets even in secure mode
		final Class<?> keyguardWidgetPager = findClass("com.android.internal.policy.impl.keyguard.KeyguardWidgetPager", lpparam.classLoader);
		
		XposedHelpers.findAndHookMethod(keyguardWidgetPager, "onLongClick", View.class, new XC_MethodReplacement() {
					@Override
					protected Object replaceHookedMethod(MethodHookParam param)
							throws Throwable {
				        // Disallow long pressing to reorder if the challenge is showing
						Object mViewStateManager = getObjectField(param.thisObject, "mViewStateManager");
						boolean isChallengeShowing = (Boolean) XposedHelpers.callMethod(mViewStateManager, "isChallengeShowing");
						boolean isChallengeOverlappingFromClass = (Boolean) XposedHelpers.callMethod(mViewStateManager, "isChallengeOverlapping");
				        boolean isChallengeOverlapping = isChallengeShowing && isChallengeOverlappingFromClass;
				       
				        Method startReordering = keyguardWidgetPager.getMethod("startReordering");  
				        if (!isChallengeOverlapping && (Boolean) startReordering.invoke(param.thisObject)) {
				            return true;
				        }
				        return false;
					}
		});

	}
}
