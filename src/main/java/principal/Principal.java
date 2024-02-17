package principal;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

// DOM
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bson.Document; // HAY QUE COMENTAR EL DE DOM Y DESCOMENTAR ESTE SEGUN LO QUE USES
import org.bson.types.ObjectId;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

// JAXB
import clasesJAXB.Localidad;
import clasesJAXB.Provincia;
import clasesJAXB.Provincias;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public class Principal {

	private static MongoDatabase database;
	private static MongoClient mongoClient;
	public final static String RUTA = System.getProperty("user.dir") + System.getProperty("file.separator") + "src"
			+ System.getProperty("file.separator") + "main" + System.getProperty("file.separator") + "resources"
			+ System.getProperty("file.separator");
	public final static String documentoXML = "capitales.xml";
	private static Provincias provincias = new Provincias();

	public static void main(String[] args) throws Exception {

		Scanner scanner = new Scanner(System.in);

		// Pedir al usuario que introduzca una capital
		System.out.println("Por favor, introduce el nombre de una capital:");

		// Leer la capital introducida por el usuario
		String nombreCapital = scanner.nextLine();

		anadirProvinciaDOM(nombreCapital);

		scanner.close();
	}

	// CONECTARSE A MONGO

	public static boolean mongoConectarOnline() {
		String connectionString = "mongodb+srv://mariomunozpequeno:4rtlus9pq7UjxKNO@cluster0.xu4apmq.mongodb.net/?retryWrites=true&w=majority";
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();
		// Create a new client and connect to the server
		mongoClient = MongoClients.create(settings);
		try {
			// Send a ping to confirm a successful connection
			database = mongoClient.getDatabase("provinciasmongo");
			database.runCommand(new Document("ping", 1));
			System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean mongoConectarLocal() {
		try {
			mongoClient = MongoClients.create();
			database = mongoClient.getDatabase("provinciasMongo");
			System.out.println("Te has conectado a MongoDB");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void mongoCerrarConexion() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	// METODO QUE TE PIDE

	private static ObjectId existeCapitalMongo(String capital) {
		if (database.getCollection("capitales").find(new Document("nombre", capital)).first() == null) {
			database.getCollection("capitales").insertOne(new Document("nombre", capital));
			System.out.println("Se almacenó la capital en MongoDB");
		} else {
			System.out.println("La capital existía en MongoDB");
		}
		return database.getCollection("capitales").find(new Document("nombre", capital)).first().getObjectId("_id");
	}

	private static void anadirProvinciaJAXB(String capital) throws JAXBException {
		mongoConectarOnline();
		JAXBContext jC = JAXBContext.newInstance(Provincias.class);
		Unmarshaller jUM = jC.createUnmarshaller();
		provincias = (Provincias) jUM.unmarshal(new File(RUTA + documentoXML));

		ObjectId oID = existeCapitalMongo(capital);
		FindIterable<Document> it = database.getCollection("poblaciones").find(new Document("capital", oID));
		Iterator iter = it.iterator();
		Document doc = new Document();

		ArrayList<Localidad> localidades = new ArrayList<Localidad>();

		while (iter.hasNext()) {
			doc = (Document) iter.next();
			Localidad localidad = new Localidad();
			localidad.setNombre(doc.getString("nombre"));
			localidades.add(localidad);
		}
		Provincia provincia = new Provincia(capital, localidades);
		provincias.getProvincia().add(provincia);

		Marshaller jM = jC.createMarshaller();
		jM.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jM.marshal(provincias, new File(RUTA + documentoXML));

	}

	// LO MISO DE ARRIBA PERO EN DOM

	private static void anadirProvinciaDOM(String capital) throws Exception {
	    mongoConectarOnline();

	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    org.w3c.dom.Document doc = builder.parse(new File(RUTA + documentoXML));

	    ObjectId oID = existeCapitalMongo(capital);
	    FindIterable<Document> it = database.getCollection("poblaciones").find(new Document("capital", oID));
	    Iterator iter = it.iterator();
	    Document docMongo = new Document();

	    ArrayList<String> localidades = new ArrayList<String>();

	    while (iter.hasNext()) {
	        docMongo = (Document) iter.next();
	        localidades.add(docMongo.getString("nombre"));
	    }

	    Node provincias = doc.getElementsByTagName("provincias").item(0);

	    Element nuevaProvincia = doc.createElement("provincia");
	    nuevaProvincia.setAttribute("nombre", capital);

	    for (String localidad : localidades) {
	        Element localidadElement = doc.createElement("localidad");
	        localidadElement.setAttribute("nombre", localidad);
	        nuevaProvincia.appendChild(localidadElement);
	    }

	    provincias.appendChild(nuevaProvincia);

	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    DOMSource source = new DOMSource(doc);
	    StreamResult result = new StreamResult(new File(RUTA + documentoXML));
	    transformer.transform(source, result);
	}
}
