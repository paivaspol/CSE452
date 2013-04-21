import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class Tester {

  public static void main(String[] args) {
    Gson gson = new GsonBuilder().create();
    String jsonStr = "{ method: 123, collection: \"abc\", data: { id: 1 } }";
    TwitterProtocolMessage result = gson.fromJson(jsonStr, TwitterProtocolMessage.class);
    System.out.println(result.getData());
    Map<String, Object> tester = new HashMap<String, Object>();
    JsonObject obj = new JsonObject();
    tester.put("test_id", "hello!");
    System.out.println(gson.toJson(new TwitterProtocolMessage("test", "col1", tester, "1", "2")));
  }
}
