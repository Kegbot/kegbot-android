/**
 *
 */
package org.kegbot.kegtap.setup;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.kegtap.Utils;
import org.kegbot.kegtap.build.BuildInfo;
import org.kegbot.kegtap.util.DeviceId;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class CheckinClient {

  private static final String TAG = CheckinClient.class.getSimpleName();
  private static final String CHECKIN_URL = "http://checkin.kegbot.org/checkin/";

  private final Context mContext;

  public CheckinClient(Context context) {
    mContext = context.getApplicationContext();
  }

  public JsonNode checkin() throws IOException {
    Log.d(TAG, "Attempting checkin: " + CHECKIN_URL);
    final HttpClient client = new DefaultHttpClient();
    final HttpPost request = new HttpPost(CHECKIN_URL);
    final HttpParams requestParams = new BasicHttpParams();

    HttpProtocolParams.setUserAgent(requestParams, Utils.getUserAgent());
    request.setParams(requestParams);

    List<NameValuePair> params = Lists.newArrayList();
    params.add(new BasicNameValuePair("kbid", DeviceId.getDeviceId(mContext)));
    params.add(new BasicNameValuePair("android_version", Build.VERSION.SDK));
    params.add(new BasicNameValuePair("android_device", Build.DEVICE));
    params.add(new BasicNameValuePair("kegbot_version", BuildInfo.BUILD_DATE_HUMAN));
    request.setEntity(new UrlEncodedFormEntity(params));

    final HttpResponse response = client.execute(request);
    Log.d(TAG, "Checkin complete");
    final String responseBody = EntityUtils.toString(response.getEntity());
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode rootNode = mapper.readValue(responseBody, JsonNode.class);
    Log.d(TAG, "Checkin response: " + rootNode);

    return rootNode;
  }

}
