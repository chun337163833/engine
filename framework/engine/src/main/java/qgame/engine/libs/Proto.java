package qgame.engine.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.Map;

/**
 * Created by kerr.
 */
public class Proto {
    public static JsonNode toJson(GeneratedMessage message){
        ObjectNode objectNode = Json.objectNode();
        Map<FieldDescriptor, Object> fields = message.getAllFields();
        for (Map.Entry<FieldDescriptor,Object> field : fields.entrySet()){
            FieldDescriptor descriptor = field.getKey();
            Object value = field.getValue();
            JavaType javaType = descriptor.getJavaType();
            String fieldName = descriptor.getName();
            if (descriptor.isRepeated()){
                //value is a list
                objectNode.set(fieldName, Json.toJsonNode(value));
                continue;
            }
            switch (javaType){
                case INT:
                    objectNode.put(fieldName, (Integer) value);
                    break;
                case LONG:
                    objectNode.put(fieldName, (Long) value);
                    break;
                case FLOAT:
                    objectNode.put(fieldName,(Float)value);
                    break;
                case DOUBLE:
                    objectNode.put(fieldName,(Double)value);
                    break;
                case BOOLEAN:
                    objectNode.put(fieldName,(Boolean)value);
                    break;
                case STRING:
                    objectNode.put(fieldName,(String)value);
                    break;
                case BYTE_STRING:
                    objectNode.put(fieldName,((ByteString)value).toByteArray());
                    break;
                case ENUM:
                    objectNode.put(fieldName,((ProtocolMessageEnum)value).getValueDescriptor().getName());
                    break;
                case MESSAGE:
                    objectNode.set(fieldName,toJson((GeneratedMessage)value));
                    break;
            }
        }
        return objectNode;
    }
}
