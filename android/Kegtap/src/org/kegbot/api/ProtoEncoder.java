package org.kegbot.api;

import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;

public class ProtoEncoder {

  /**
   * Assigns all fields in {@link JsonNode} instance to a {@link Builder}.
   *
   * @param builder
   *          the builder to be populated
   * @param root
   *          the JSON object
   * @return the original builder, populated with any fields that were
   *         discovered
   */
  public static Builder toProto(final Builder builder, final JsonNode root) {
    Descriptor type = builder.getDescriptorForType();
    for (final FieldDescriptor fieldDesc : type.getFields()) {
      final String attrName = fieldDesc.getName();
      final JsonNode node = root.get(attrName);

      if (node == null) {
        continue;
      }

      if (node.isNull()) {
        continue;
      }

      if (fieldDesc.isRepeated()) {
        final Iterator<JsonNode> iter = node.getElements();
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

  private static Object toJavaObj(final Builder builder,
      final FieldDescriptor fieldDesc, final JsonNode node) {
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
      try {
        value = node.getBinaryValue();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
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

}
