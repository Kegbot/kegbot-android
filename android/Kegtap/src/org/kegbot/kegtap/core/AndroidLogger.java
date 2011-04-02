package org.kegbot.kegtap.core;

import org.kegbot.core.Logger;

import android.util.Log;

public class AndroidLogger implements Logger {

  @Override
  public void v(String tag, String msg) {
    Log.v(tag, msg);
  }

  @Override
  public void v(String tag, String msg, Throwable tr) {
    Log.v(tag, msg, tr);
  }

  @Override
  public void d(String tag, String msg) {
    Log.d(tag, msg);
  }

  @Override
  public void d(String tag, String msg, Throwable tr) {
    Log.d(tag, msg, tr);
  }

  @Override
  public void i(String tag, String msg) {
    Log.i(tag, msg);
  }

  @Override
  public void i(String tag, String msg, Throwable tr) {
    Log.i(tag, msg, tr);
  }

  @Override
  public void w(String tag, String msg) {
    Log.w(tag, msg);
  }

  @Override
  public void w(String tag, String msg, Throwable tr) {
    Log.w(tag, msg, tr);
  }

  @Override
  public void e(String tag, String msg) {
    Log.e(tag, msg);
  }

  @Override
  public void e(String tag, String msg, Throwable tr) {
    Log.e(tag, msg, tr);
  }

}
