package pt.unl.fct.di.adc.firstwebapp.util;

public class LogoutData {
	
	public String username;
	
	public LogoutData() {}

	public LogoutData(String username) {
		this.username = username;
	}

	public boolean isValid() {
		//Checks for missing information
		if (this.username == null)
			return false;

		//Checks for empty data
		return this.username.length() != 0;
	}

}
