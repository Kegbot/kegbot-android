package org.kegbot.kegtap.util;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;

/**
 * Describes a kegbot system.
 */
public class KegbotDescriptor {

  String mName;
  Uri mUrl;

  public KegbotDescriptor(String name, Uri url) {
    mName = name;
    mUrl = url;
  }
  
  public String getName() {
    return mName;
  }
  
  public Uri getUrl() {
    return mUrl;
  }
  
  public Uri getApiUrl() {
    return mUrl.buildUpon()
        .appendEncodedPath("api")
        .build();
  }
  
  public static String[] getNames(List<KegbotDescriptor> list) {
    ArrayList<String> returnList = new ArrayList<String>(list.size());
    for (KegbotDescriptor descriptor : list) {
      returnList.add(descriptor.getName());
    }
    return returnList.toArray(new String[list.size()]);
  }
  
  public static String[] getEntryValues(List<KegbotDescriptor> list) {
    ArrayList<String> returnList = new ArrayList<String>(list.size());
    for (KegbotDescriptor descriptor : list) {
      returnList.add(descriptor.getUrl().toString());
    }
    return returnList.toArray(new String[list.size()]);
  }

  public static Uri getApiUrl(String kegbotUrl) {
    return Uri.parse(kegbotUrl).buildUpon()
        .appendEncodedPath("api")
        .build();
  }
}
