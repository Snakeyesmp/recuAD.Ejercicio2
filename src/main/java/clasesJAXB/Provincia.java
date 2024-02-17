package clasesJAXB;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

public class Provincia {

	private String nombre;
	private ArrayList<Localidad> localidad;

	public Provincia() {

	}

	public Provincia(String nombre, ArrayList<Localidad> localidad) {
		this.nombre = nombre;
		this.localidad = localidad;
	}

	@XmlAttribute()
	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	@XmlElement(name = "localidad")
	public ArrayList<Localidad> getLocalidad() {
		return localidad;
	}

	public void setLocalidad(ArrayList<Localidad> localidad) {
		this.localidad = localidad;
	}

}
