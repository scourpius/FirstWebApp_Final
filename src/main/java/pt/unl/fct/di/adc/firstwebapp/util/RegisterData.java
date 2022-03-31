package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {

	//Required information
	public String username, email, name, password, confirmation;
		
	//Optional information
	public String telefone, telemovel, morada, nif;

	public RegisterData() { }

	public RegisterData(String username, String email,  String name, String password, String confirmation, String telefone, String telemovel, String morada, String nif) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.telefone = telefone;
		this.telemovel = telemovel;
		this.morada = morada;
		this.nif = nif;
	}

	public boolean validRegistration() {
		// Checks for missing info
		if (this.username == null || this.password == null || this.confirmation == null || this.email == null || this.name == null)
			return false;

		// Checks for empty data
		if(this.username.length() == 0 || this.password.length() == 0 || this.confirmation == null || this.email.length() == 0 || this.name.length() == 0)
			return false;
		
		//Check if password contains the minimum requirements (At least 6 characters long, 1 digit and 1 upper case letter)
		if (this.password.length() < 6)
			return false;
		
		boolean hasDigit = false, hasUpper = false;
		int i = 0;
		char aux;
		while(i < this.password.length() && !(hasDigit && hasUpper)) {
			aux = this.password.charAt(i);
			if(Character.isDigit(aux)) hasDigit = true;
			if(Character.isUpperCase(aux)) hasUpper = true;
			i++;
		}
		
		if (!hasDigit || !hasUpper)
			return false;
				
		//Set these attributes as undefined if not filled
		if(this.telefone == null)
			telefone = "INDEFINIDO";
		if(this.telemovel == null)
			telemovel = "INDEFINIDO";
		if(this.morada == null)
			morada = "INDEFINIDO";
		if(this.nif == null)
			nif = "INDEFINIDO";

		// Check for confirmation
		return this.password.equals(this.confirmation);
	}
}