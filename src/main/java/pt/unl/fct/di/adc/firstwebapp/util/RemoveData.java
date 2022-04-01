package pt.unl.fct.di.adc.firstwebapp.util;

public class RemoveData {

	public String remover, removed;

	public RemoveData() {}

	public RemoveData(String remover, String removed) {
		this.remover = removed;
		this.removed = removed;
	}

	public boolean isValid() {
		//Checks for missing information
		if (this.remover == null || this.removed == null)
			return false;

		//Checks for empty data
		return (this.remover.length() != 0 && this.removed.length() != 0);
	}

}