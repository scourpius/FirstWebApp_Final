package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginData {

	public String username, password;

	public LoginData() {}

	public LoginData(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public boolean isValid() {
		//Checks for missing information
		if (this.username == null || this.password == null)
			return false;

		//Checks for empty data
		if (this.username.length() == 0 || this.password.length() == 0)
			return false;

		return true;
	}

}