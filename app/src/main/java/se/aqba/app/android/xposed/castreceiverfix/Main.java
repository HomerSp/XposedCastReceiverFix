package se.aqba.app.android.xposed.castreceiverfix;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.res.TypedArray;
import android.media.MediaDrm;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String TAG = "XposedCastReceiverFix";

    private static int[] AndroidManifestUsesSdk;
    private static int AndroidManifestUsesSdk_minSdkVersion = 0;
    private static Field arrayRsrcsField;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        Log.d(TAG, "initZygote");

        findAndHookMethod(TypedArray.class, "peekValue", Integer.TYPE, mPeekValueOverride);

        findAndHookMethod("com.android.server.WifiService", null, "enforceAccessPermission", mMultiCastOverride);
        findAndHookMethod("com.android.server.WifiService", null, "enforceMulticastChangePermission", mMultiCastOverride);
        findAndHookMethod("com.android.server.wifi.WifiService", null, "enforceAccessPermission", mMultiCastOverride);
        findAndHookMethod("com.android.server.wifi.WifiService", null, "enforceMulticastChangePermission", mMultiCastOverride);
    }

    XC_MethodHook mPeekValueOverride = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            //Log.d(TAG, "beforeHookedMethod peekValue");
        }
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if((Integer)param.args[0] != AndroidManifestUsesSdk_minSdkVersion)
                return;

            final TypedArray array = (TypedArray)param.thisObject;
            int[] rsrcs = (int [])arrayRsrcsField.get(array);

            if(rsrcs[0] != AndroidManifestUsesSdk[0])
                return;

            final TypedValue value = (TypedValue)param.getResult();
            if(value.type != TypedValue.TYPE_STRING) {
                if(value.data >= 19) {
                    Log.d(TAG, "Overriding min SDK to 15 from " + value.data);

                    value.data = 15;
                    param.setResult(value);
                }
            }
        }
    };
    XC_MethodHook mMultiCastOverride = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.setResult(null);
        }
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

        }
    };


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

    }

    static {
        try {
            Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");

            Field usesSdkField = styleableClass.getDeclaredField("AndroidManifestUsesSdk");
            AndroidManifestUsesSdk = (int[])usesSdkField.get(null);

            Field minSdkVersionField = styleableClass.getDeclaredField("AndroidManifestUsesSdk_minSdkVersion");
            AndroidManifestUsesSdk_minSdkVersion = minSdkVersionField.getInt(null);

            arrayRsrcsField = TypedArray.class.getDeclaredField("mRsrcs");
            arrayRsrcsField.setAccessible(true);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
