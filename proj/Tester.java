import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Tester {

  public static void main(String[] args) {
    Gson gson = new GsonBuilder().create();
    String jsonStr = "{ method: 123, collection: \"abc\", data: { id: 1 } }";
    TwitterProtocolMessage result = gson.fromJson(jsonStr, TwitterProtocolMessage.class);
    System.out.println(result.getData());

    System.out.println(gson.toJson(new TwitterProtocolMessage("test", "col1", new HashMap<String, Object>(),
        new AnotherClass(1, 2))));
  }
}
