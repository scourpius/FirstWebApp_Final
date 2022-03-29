package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String password;
	public String confirmation;
	public String email;
	public String name;

	public RegisterData() { }

	public RegisterData(String username, String password, String confirmation, String email, String name) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
	}

	public boolean validRegistration() {
		// Checks for missing info
		if (this.username == null || this.password == null || this.confirmation == null || this.email == null || this.name == null)
			return false;

		// Checks for empty data
		if(this.username.length() == 0 || this.password.length() == 0 || this.confirmation == null || this.email.length() == 0 || this.name.length() == 0)
			return false;


		// Check for confirmation
		return this.password.equals(this.confirmation);
	}
}