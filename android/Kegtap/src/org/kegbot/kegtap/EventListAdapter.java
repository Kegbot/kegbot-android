package org.kegbot.kegtap;

import java.util.LinkedHashMap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class EventListAdapter extends BaseAdapter {
  
  private final LinkedHashMap<String, String> entries = new LinkedHashMap<String, String>();
  
  private Context context;

  public EventListAdapter(Context context) {
    super();
    this.context = context;
    entries.put("foo", "bar");
    entries.put("foo2", "bar2");
  }

  @Override
  public int getCount() {
    return entries.size();
  }

  @Override
  public Object getItem(int arg0) {
    return entries.entrySet().toArray()[arg0];
  }

  @Override
  public long getItemId(int arg0) {
    return arg0;
  }

  @Override
  public View getView(int index, View cellRenderer, ViewGroup parent) {
    KBEventView view = null;
    if (cellRenderer == null) {
      view = new KBEventView(context);
    } else {
      view = (KBEventView) cellRenderer;
    }
    view.display(index, false);
    return view;
  }

}
