package com.cassens.autotran;

import androidx.lifecycle.GeneratedAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MethodCallsLogger;
import java.lang.Override;

public class AutoTranApplication_AppLifecycleObserver_LifecycleAdapter implements GeneratedAdapter {
  final AutoTranApplication.AppLifecycleObserver mReceiver;

  AutoTranApplication_AppLifecycleObserver_LifecycleAdapter(AutoTranApplication.AppLifecycleObserver receiver) {
    this.mReceiver = receiver;
  }

  @Override
  public void callMethods(LifecycleOwner owner, Lifecycle.Event event, boolean onAny,
      MethodCallsLogger logger) {
    boolean hasLogger = logger != null;
    if (onAny) {
      return;
    }
    if (event == Lifecycle.Event.ON_CREATE) {
      if (!hasLogger || logger.approveCall("onCreate", 1)) {
        mReceiver.onCreate();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_START) {
      if (!hasLogger || logger.approveCall("onStart", 1)) {
        mReceiver.onStart();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_RESUME) {
      if (!hasLogger || logger.approveCall("onMoveToForeground", 1)) {
        mReceiver.onMoveToForeground();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_PAUSE) {
      if (!hasLogger || logger.approveCall("onMoveToBackground", 1)) {
        mReceiver.onMoveToBackground();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_STOP) {
      if (!hasLogger || logger.approveCall("onStop", 1)) {
        mReceiver.onStop();
      }
      return;
    }
    if (event == Lifecycle.Event.ON_DESTROY) {
      if (!hasLogger || logger.approveCall("onDestroy", 1)) {
        mReceiver.onDestroy();
      }
      return;
    }
  }
}
