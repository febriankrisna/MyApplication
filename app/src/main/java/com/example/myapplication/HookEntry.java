package com.example.myapplication;

import android.app.Application;
import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.VCB")) return;

        XposedBridge.log("MyHook: Loaded app = " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context ctx = (Context) param.args[0];
                        ClassLoader cl = ctx.getClassLoader();
                        XposedBridge.log("MyHook: Application.attach hooked, CL=" + cl);

                        try {
                            // === ProtectedMainApplication ===
                            Class<?> appCls = cl.loadClass("com.VCB.main.ProtectedMainApplication");

                            // Hook IBpce (return byte[])
                            try {
                                XposedHelpers.findAndHookMethod(appCls, "IBpce", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        byte[] result = (byte[]) param.getResult();
                                        XposedBridge.log("MyHook: IBpce() -> " + (result != null ? Arrays.toString(result) : "null"));
                                    }
                                });
                            } catch (Throwable t) {
                                XposedBridge.log("MyHook: gagal hook IBpce -> " + t);
                            }

                            // Hook fGbC (void)
                            try {
                                XposedHelpers.findAndHookMethod(appCls, "fGbC", new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        XposedBridge.log("MyHook: fGbC() called");
                                    }
                                });
                            } catch (Throwable t) {
                                XposedBridge.log("MyHook: gagal hook fGbC -> " + t);
                            }

                            // Hook qAm(Object) → bypass crash
                            try {
                                XposedHelpers.findAndHookMethod(appCls, "qAm", Object.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        Object arg = param.args[0];
                                        if (arg instanceof byte[]) {
                                            XposedBridge.log("MyHook: qAm() intercepted, arg=" + Arrays.toString((byte[]) arg));
                                        } else {
                                            XposedBridge.log("MyHook: qAm() intercepted, arg=" + arg);
                                        }
                                        // bypass native
//                                        param.setResult(null);
//                                        XposedBridge.log("MyHook: qAm() bypassed");
                                    }
                                });
                            } catch (Throwable t) {
                                XposedBridge.log("MyHook: gagal hook qAm -> " + t);
                            }

                            // === ProtectedMainApplication$ProtectedAppComponentFactory.xqgsx(Object, Throwable) ===
                            try {
                                XposedHelpers.findAndHookMethod(
                                        "com.VCB.main.ProtectedMainApplication$ProtectedAppComponentFactory",
                                        cl,
                                        "xqgsx",
                                        Object.class,
                                        Throwable.class,
                                        new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                Throwable th = (Throwable) param.args[1];
                                                XposedBridge.log("MyHook: xqgsx() exception=" + th);
                                                if (th != null) {
                                                    XposedBridge.log("MyHook: stacktrace -> " + android.util.Log.getStackTraceString(th));
                                                }
                                            }
                                        }
                                );
                            } catch (Throwable t) {
                                XposedBridge.log("MyHook: gagal hook xqgsx -> " + t);
                            }

                            // === ProtectedMainApplication$MainApplication$onCreate$1$MainApplication.a()Z ===
                            try {
                                XposedHelpers.findAndHookMethod(
                                        "com.VCB.main.ProtectedMainApplication$MainApplication$onCreate$1$MainApplication",
                                        cl,
                                        "a",
                                        new XC_MethodHook() {
                                            @Override
                                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                                boolean orig = (boolean) param.getResult();
                                                XposedBridge.log("MyHook: MainApplication.a() called, orig=" + orig);

                                                // ⚠️ force return false agar init lanjut
                                                param.setResult(false);
                                                XposedBridge.log("MyHook: MainApplication.a() forced result=false");
                                            }
                                        }
                                );
                            } catch (Throwable t) {
                                XposedBridge.log("MyHook: gagal hook MainApplication.a() -> " + t);
                            }

                        } catch (Throwable t) {
                            XposedBridge.log("MyHook: gagal load ProtectedMainApplication -> " + t);
                        }
                    }
                }
        );
    }
}
