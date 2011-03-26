package org.kegbot.api;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.proto.Api.KegSet;
import org.kegbot.proto.Api.SessionDetail;

public class ApiTester {

  /**
   * @param args
   * @throws KegbotApiException
   */
  public static void main(String[] args) throws KegbotApiException {
    KegbotApi api = new KegbotApiImpl(new DefaultHttpClient(),
        "http://kegbot.net/sfo/api");
    api.getDrinkDetail("1");

    api.getKegDetail("2");

    KegSet response = api.getAllKegs();
    System.out.println(response);


    SessionDetail session = api.getSessionDetail("1");
    System.out.println(session);

    System.out.println(api.getUserEvents("mikey"));

    System.out.println(api.getUserDrinks("mikey"));

  }

}
