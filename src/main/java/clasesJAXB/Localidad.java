package clasesJAXB;

import jakarta.xml.bind.annotation.XmlValue;

public class Localidad {
	private String nombre;
	
	public Localidad() {
		
	}
	
	@XmlValue()
	public String getNombre() {
		return this.nombre;
	}
	
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
}
