/**
 * 
 * @author leelee
 *
 */
public class User extends Entry {
	
	private static final String USERNAME_KEY = "username";
	private static final String NAME_KEY = "name";
	private static final String PASSWORD_KEY = "password";
	private static final String IS_LOGIN_KEY = "islogin";
	
	public User(int machineId) {
		super(machineId);
	}
	
	public String getUserName() {
		return jsonObject.get(USERNAME_KEY).getAsString();
	}
	
	public String getName() {
		return jsonObject.get(NAME_KEY).getAsString();
	}
	
	public String getPassword() {
		return jsonObject.get(PASSWORD_KEY).getAsString();
	}
	
	public boolean isLogin() {
		return jsonObject.get(IS_LOGIN_KEY).getAsBoolean();
	}
	
	public void setUserName(String username) {
		jsonObject.addProperty(USERNAME_KEY, username);
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
}
