package org.kegbot.kegtap.core;

public interface Logger {

  public void v(String tag, String msg);

  public void v(String tag, String msg, Throwable tr);

  public void d(String tag, String msg);

  public void d(String tag, String msg, Throwable tr);

  public void i(String tag, String msg);

  public void i(String tag, String msg, Throwable tr);

  public void w(String tag, String msg);

  public void w(String tag, String msg, Throwable tr);

  public void e(String tag, String msg);

  public void e(String tag, String msg, Throwable tr);

}
