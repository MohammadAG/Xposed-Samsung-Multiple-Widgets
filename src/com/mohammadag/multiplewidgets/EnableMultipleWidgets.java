package com.mohammadag.multiplewidgets;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class EnableMultipleWidgets implements IXposedHookLoadPackage {

	private static boolean mEasyUxDoesNotExist = false;

	private static final String[] METHODS_TO_HOOK = { 
		"addDefaultWidgets",
		"addWidgetsFromSettings",
		"shouldEnableAddWidget",
		"checkAppWidgetConsistency"
	};

	public boolean mMultipleLockBooleanExists = true;

	protected boolean mShouldForceCameraOn = true;

	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		// Allow the user to switch between secure camera or Samsung's shortcuts.
		if (lpparam.packageName.equals("com.android.settings")) {
			try {
				Class<?> LockScreenSettings = findClass("com.android.settings.LockScreenSettings", lpparam.classLoader);
				try {
					findAndHookMethod(LockScreenSettings, "onResume", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) {
							try {
								SwitchPreference mShortCameraWidget = (SwitchPreference) getObjectField(param.thisObject, "mShortCameraWidget");
								mShortCameraWidget.setEnabled(true);
							} catch (NoSuchFieldError e) {}
						}
					});
				} catch (NoSuchMethodError e) {}
			} catch (ClassNotFoundError e) {}
			return;
		}

		if (!lpparam.packageName.equals("android"))
			return;
		
		final EasyUxHook hook = new EasyUxHook();

		try {
			final Class<?> KeyguardHostView = findClass("com.android.internal.policy.impl.keyguard.KeyguardHostView", lpparam.classLoader);
			XposedBridge.hookAllConstructors(KeyguardHostView, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (mShouldForceCameraOn) {
						Context context = (Context) param.args[0];
						if (context != null)
							Settings.System.putInt(context.getContentResolver(), "kg_enable_camera_widget", 1);
					}
				}
				@Override
				protected void afterHookedMethod(MethodHookParam param) {
					try {
						setBooleanField(param.thisObject, "mIsEasyUxOn", false);
					} catch (NoSuchFieldError e) {}
					
					try {
						setBooleanField(param.thisObject, "mIsMultipleLockOn", true);
					} catch (NoSuchFieldError e) {
						mMultipleLockBooleanExists = false;
					}
				}
			});
			
			if (!mEasyUxDoesNotExist) {
				for (String methodName : METHODS_TO_HOOK) {
					findAndHookMethod(KeyguardHostView, methodName, hook);
				}
			}

			// First appeared in leaked 4.3 on Note 2 | GT-N7100
			try {
				findAndHookMethod(KeyguardHostView, "shouldEnableAddWidget", XC_MethodReplacement.returnConstant(true));
			} catch (NoSuchMethodError e) {}
		} catch (ClassNotFoundError e) {}

		// Allow reordering of lock screen widgets even in secure mode
		final Class<?> keyguardWidgetPager = findClass("com.android.internal.policy.impl.keyguard.KeyguardWidgetPager", lpparam.classLoader);
		findAndHookMethod(keyguardWidgetPager, "onLongClick", View.class, new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
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
		
		try {
			findAndHookMethod(keyguardWidgetPager, "onTouchEvent", MotionEvent.class, new TouchEventHook());
			findAndHookMethod(keyguardWidgetPager, "onInterceptTouchEvent", MotionEvent.class, new InterceptTouchEventHook());
		} catch (NoSuchMethodError e) {}
		
		try {
			// Samsung, you seriously fucked up the Android lockscreen.
			IsSecuredHook securedHook = new IsSecuredHook();
			Class<?> SlidingChallengeLayout = findClass("com.android.internal.policy.impl.keyguard.SlidingChallengeLayout", lpparam.classLoader);
			XposedBridge.hookAllConstructors(SlidingChallengeLayout, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					XposedHelpers.setBooleanField(param.thisObject, "mIsSecured", false);
				}
			});
			
			try {
				findAndHookMethod(SlidingChallengeLayout, "setIsSecured", boolean.class, XC_MethodReplacement.DO_NOTHING);
				findAndHookMethod(SlidingChallengeLayout, "dispatchTouchEvent", MotionEvent.class, securedHook);
				findAndHookMethod(SlidingChallengeLayout, "onInterceptTouchEvent", MotionEvent.class, securedHook);
				findAndHookMethod(SlidingChallengeLayout, "onTouchEvent", MotionEvent.class, securedHook);
				findAndHookMethod(SlidingChallengeLayout, "onMeasure", int.class, int.class, securedHook);
				
			} catch (NoSuchMethodError e) {
				// Will happen on whatever pre-4.3 devices. You know, before Samsung lost their shit.
			}
			
		} catch (ClassNotFoundError e) {
			// Shouldn't happen
		}
	}

	class EasyUxHook extends XC_MethodHook {
		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			try {
				setBooleanField(param.thisObject, "mIsEasyUxOn", false);
			} catch (NoSuchFieldError e) {}
			
			if (mMultipleLockBooleanExists) {
				try {
					setBooleanField(param.thisObject, "mIsMultipleLockOn", true);
				} catch (NoSuchFieldError e) {
					e.printStackTrace();
					mMultipleLockBooleanExists = false;
				}
			}
		};
	};
	
	class TouchEventHook extends XC_MethodReplacement {
		@Override
		protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
			MotionEvent ev = (MotionEvent) param.args[0];
			boolean captureUserInteraction =
					(Boolean) XposedHelpers.callMethod(param.thisObject, "captureUserInteraction", ev);
			boolean superBool = (Boolean) callSuperMethod(param.thisObject, "onTouchEvent", ev);	
			return captureUserInteraction || superBool;
		}
	}
	
	class InterceptTouchEventHook extends XC_MethodReplacement {	
		@Override
		protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
			MotionEvent ev = (MotionEvent) param.args[0];
			boolean captureUserInteraction =
					(Boolean) XposedHelpers.callMethod(param.thisObject, "captureUserInteraction", ev);
			boolean superBool = (Boolean) callSuperMethod(param.thisObject, "onInterceptTouchEvent", ev);
			
			return captureUserInteraction || superBool;
		}
	}
	
	class IsSecuredHook extends XC_MethodHook {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			setBooleanField(param.thisObject, "mIsSecured", false);
		}
	}
	
    private Object callSuperMethod(Object obj, String methodName, Object... objects) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException { 
        Class<?> SuperClass = obj.getClass().getSuperclass();
        Method method = XposedHelpers.findMethodBestMatch(SuperClass, methodName, MotionEvent.class);
        return XposedBridge.invokeOriginalMethod(method, obj, objects);    		
    }  
}
