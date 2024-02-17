package clasesJAXB;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="provincias")
public class Provincias {
		
	private ArrayList<Provincia> provincia;

	public Provincias() {
	}

	public Provincias(ArrayList<Provincia> provincia) {
		this.provincia = provincia;
	}
	@XmlElement(name="provincia")
	public ArrayList<Provincia> getProvincia() {
		return provincia;
	}
	
	public void setProvincia(ArrayList<Provincia> provincia) {
		this.provincia = provincia;
	}
	
}
