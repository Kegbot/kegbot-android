package org.kegbot.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkDetailHtml;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SystemEventHtml;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.User;

import android.util.Pair;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class KegbotApiImpl implements KegbotApi {

  private final HttpClient httpClient;
  private final String baseUrl;

  public KegbotApiImpl(HttpClient httpClient, String baseUrl) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
  }

  private JsonNode getJsonResponse(HttpResponse response)
      throws KegbotApiException {
    final Header header = response.getFirstHeader("Content-type");
    if (header == null) {
      throw new KegbotApiException("No content-type header.");
    }
    final String contentType = header.getValue();
    if (!"application/json".equals(contentType)) {
      throw new KegbotApiException("Unknown content-type: " + contentType);
    }
    try {
      // String body = streamToString(response.getEntity().getContent());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readValue(response.getEntity().getContent(),
          JsonNode.class);
      return rootNode;
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }
  }

  private String streamToString(InputStream stream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
      builder.append('\n');
    }
    return builder.toString();
  }

  private HttpResponse doGet(String path, Pair<String, String>[] params)
      throws KegbotApiException {
    final String url = baseUrl + path;
    HttpGet request = new HttpGet(url);
    return execute(request);
  }

  private HttpResponse execute(HttpUriRequest request)
      throws KegbotApiException {
    HttpResponse response;

    try {
      response = httpClient.execute(request);
    } catch (ClientProtocolException e) {
      throw new KegbotApiException(e);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }

    return response;
  }

  @Override
  public List<Keg> getAllKegs() throws KegbotApiException {
    HttpResponse response = doGet("/kegs", null);
    System.out.println(response);
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public List<SoundEvent> getAllSoundEvents() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public List<TapDetail> getAllTaps() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  private Object toJavaObj(final Builder builder,
      final FieldDescriptor fieldDesc, final JsonNode node) throws IOException {
    Object value;
    switch (fieldDesc.getJavaType()) {
    case MESSAGE:
      final Builder subBuilder = builder.newBuilderForField(fieldDesc);
      value = toProto(subBuilder, node).build();
      break;
    case BOOLEAN:
      value = Boolean.valueOf(node.getBooleanValue());
      break;
    case BYTE_STRING:
      value = node.getBinaryValue();
      break;
    case DOUBLE:
      value = Double.valueOf(node.getDoubleValue());
      break;
    case FLOAT:
      value = Float.valueOf(Double.valueOf(node.getDoubleValue()).floatValue());
      break;
    case ENUM:
      value = fieldDesc.getEnumType().findValueByName(node.getTextValue());
      break;
    case INT:
      value = Integer.valueOf(node.getIntValue());
      break;
    case LONG:
      value = Long.valueOf(node.getLongValue());
      break;
    case STRING:
      value = node.getTextValue();
      break;
    default:
      throw new IllegalArgumentException();
    }
    return value;

  }

  private Builder toProto(final Builder builder, final JsonNode root)
      throws IOException {
    Descriptor type = builder.getDescriptorForType();
    for (final FieldDescriptor fieldDesc : type.getFields()) {
      final String attrName = fieldDesc.getName();
      final JsonNode node = root.get(attrName);

      if (node == null) {
        // Missing.
        if (fieldDesc.isRequired()) {
          // Fail fast.
        }
        continue;
      }

      if (fieldDesc.isRepeated()) {
        Iterator<JsonNode> iter = node.getElements();
        while (iter.hasNext()) {
          builder.addRepeatedField(fieldDesc,
              toJavaObj(builder, fieldDesc, iter.next()));
        }
      } else {
        builder.setField(fieldDesc, toJavaObj(builder, fieldDesc, node));
      }
    }
    return builder;
  }

  private Message jsonWrapper(String url, Message.Builder builder)
      throws KegbotApiException {
    HttpResponse response = doGet(url, null);
    JsonNode root = getJsonResponse(response).get("result");
    try {
      toProto(builder, root);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }
    Message result = builder.build();
    System.out.println(result.toString());
    return result;
  }

  @Override
  public DrinkDetail getDrinkDetail(String id) throws KegbotApiException {
    return (DrinkDetail) jsonWrapper("/drinks/" + id, DrinkDetail.newBuilder());
  }

  @Override
  public KegDetail getKegDetail(String id) throws KegbotApiException {
    return (KegDetail) jsonWrapper("/kegs/" + id, KegDetail.newBuilder());
  }

  @Override
  public List<Drink> getKegDrinks(String kegId) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public List<SystemEvent> getKegEvents(String kegId) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public List<Session> getKegSessions(String kegId) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public String getLastDrinkId() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Collection<Drink> getRecentDrinks() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Collection<DrinkDetailHtml> getRecentDrinksHtml()
      throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Collection<SystemEvent> getRecentEvents() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public List<SystemEventHtml> getRecentEventsHtml() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Session getSession(String id) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public SessionDetail getSessionDetail(String id) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public TapDetail getTapDetail(String tapName) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Collection<ThermoLog> getThermoSensorLogs(String sensorId)
      throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public Collection<ThermoSensor> getThermoSensors() throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

  @Override
  public User getUser(String username) throws KegbotApiException {
    throw new KegbotApiException("Not implemented.");
  }

}
