import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

/**
 * 
 * @author leelee
 *
 */
public class User extends Entry {
	private static final String NAME_KEY = "name";
	private static final String PASSWORD_KEY = "password";
	private static final String IS_LOGIN_KEY = "islogin";
	private static final String USERNAME_KEY = "username";
	
	private Map<User, Long> followerToTime = new HashMap<User, Long>();
	
	public User(int machineId) {
		super(machineId);
	}
	
	public User(JsonObject jsonObject) {
		// TODO(leelee): to be changed in the future
		super(null, jsonObject);
	}
	
	public String getName() {
		return jsonObject.get(NAME_KEY).getAsString();
	}
	
	public String getPassword() {
		return jsonObject.get(PASSWORD_KEY).getAsString();
	}
	
	public String getUsername() {
		return jsonObject.get(USERNAME_KEY).getAsString();
	}
	
	public boolean isLogin() {
		return jsonObject.get(IS_LOGIN_KEY).getAsBoolean();
	}
	
	public void setName(String name) {
		jsonObject.addProperty(NAME_KEY, name);
	}
	
	public void setPassword(String password) {
		jsonObject.addProperty(PASSWORD_KEY, password);
	}
	
	public void setIsLogin(boolean isLogin) {
		jsonObject.addProperty(IS_LOGIN_KEY, isLogin);
	}
	
	public void setUsername(String username) {
		jsonObject.addProperty(USERNAME_KEY, username);
	}
	
	public Map<User, Long> getFollowersInfo() {
		return new HashMap<User, Long>(followerToTime);
	}
	
	/**
	 * 
	 * @param followeToTime is expected to map from user to time in long.
	 */
	public void setFollowersInfo(Map<User, Long> followerToTime) {
		this.followerToTime = followerToTime;
	}
}
