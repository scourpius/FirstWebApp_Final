package pt.unl.fct.di.adc.firstwebapp.util;

public class AttributeUpdateData {

	public String changer, changed, email, name, telefone, telemovel, morada, nif, state, role;
	
	public AttributeUpdateData() {}
	
	public AttributeUpdateData(String changer, String changed, String email, String name, String telefone, String telemovel, String morada, String nif, String state, String role) {
		this.changer = changer;
		this.changed = changed;
		this.email = email;
		this.name = name;
		this.telefone = telefone;
		this.telemovel = telemovel;
		this.morada = morada;
		this.nif = nif;
		this.state = state;
		this.role = role;
	}
	
	public boolean isValid() {
		//Checks if users are null
		if (this.changer == null || this.changed == null)
			return false;
		
		//Checks if information is all empty
		return !(this.email == null && this.name == null && this.telefone == null && this.telemovel == null && this.morada == null && this.nif == null && this.state == null && this.role == null);
	}
	
	public boolean hasRole(String changerRole, String changedRole) {
		if (this.changer.equals(this.changed)) {
			if (changerRole.equals("USER"))
				return (this.email == null && this.name == null && this.role == null);
			else
				return this.role == null;
		}
		
		switch(changerRole) {
		case "USER":
			return false;
			
		case "GBO":
			return (changedRole.equals("USER") && this.role == null);
			
		case "GS":
			if (changedRole.equals("GBO"))
				return this.role == null;
			if (changedRole.equals("USER"))
				return ( this.role == null || this.role.equals("GBO") );
			return false;
		default:
			return true;
		}
	}
}
