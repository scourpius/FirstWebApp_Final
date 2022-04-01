package pt.unl.fct.di.adc.firstwebapp.util;

public class PasswordUpdateData {
	
	public String username, oldPassword, newPassword, confirmation;
	
	public PasswordUpdateData() {}
	
	public PasswordUpdateData(String username, String oldPassword, String newPassword, String confirmation) {
		this.username = username;
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
		this.confirmation = confirmation;
	}

	public boolean isValid() {
		//Checks for missing information
		if (this.username == null || this.oldPassword == null || this.newPassword == null || this.confirmation == null)
			return false;

		//Checks for empty data
		if (this.username.length() == 0 || this.oldPassword.length() == 0 || this.newPassword.length() == 0 || this.confirmation.length() == 0)
			return false;

		return this.newPassword.equals(this.confirmation);
	}

}
