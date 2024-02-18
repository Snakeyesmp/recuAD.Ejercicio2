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

	private static MongoDatabase database; // Base de datos MongoDB
	private static MongoClient mongoClient; // Cliente MongoDB
	public final static String RUTA = System.getProperty("user.dir") + System.getProperty("file.separator") + "src"
			+ System.getProperty("file.separator") + "main" + System.getProperty("file.separator") + "resources"
			+ System.getProperty("file.separator");
	public final static String documentoXML = "capitales.xml"; // Nombre del archivo XML
	private static Provincias provincias = new Provincias(); // Objeto que representa las provincias en JAXB

	public static void main(String[] args) throws Exception {

		Scanner scanner = new Scanner(System.in);

		// Pedir al usuario que introduzca una capital
		System.out.println("Por favor, introduce el nombre de una capital:");

		// Leer la capital introducida por el usuario
		String nombreCapital = scanner.nextLine();

		// Añadir la capital introducida por el usuario al XML usando DOM
		anadirProvinciaDOM(nombreCapital);

		scanner.close();
	}

	// Método para conectarse a MongoDB en línea
	public static boolean mongoConectarOnline() {
		String connectionString = "mongodb+srv://mariomunozpequeno:4rtlus9pq7UjxKNO@cluster0.xu4apmq.mongodb.net/?retryWrites=true&w=majority";
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();
		// Crear un nuevo cliente y conectar al servidor
		mongoClient = MongoClients.create(settings);
		try {
			// Enviar un ping para confirmar una conexión exitosa
			database = mongoClient.getDatabase("provinciasmongo");
			database.runCommand(new Document("ping", 1));
			System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Método para conectarse a MongoDB localmente
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

	// Método para cerrar la conexión a MongoDB
	public static void mongoCerrarConexion() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	// Método para verificar si una capital existe en MongoDB y si no, añadirla
	private static ObjectId existeCapitalMongo(String capital) {
		if (database.getCollection("capitales").find(new Document("nombre", capital)).first() == null) {
			database.getCollection("capitales").insertOne(new Document("nombre", capital));
			System.out.println("Se almacenó la capital en MongoDB");
		} else {
			System.out.println("La capital existía en MongoDB");
		}
		return database.getCollection("capitales").find(new Document("nombre", capital)).first().getObjectId("_id");
	}

	// Método para añadir una provincia al XML usando JAXB
	private static void anadirProvinciaJAXB(String capital) throws JAXBException {
		mongoConectarOnline();
		JAXBContext jC = JAXBContext.newInstance(Provincias.class);
		Unmarshaller jUM = jC.createUnmarshaller();
		provincias = (Provincias) jUM.unmarshal(new File(RUTA + documentoXML));

		ObjectId oID = existeCapitalMongo(capital);
		FindIterable<Document> it = database.getCollection("poblaciones").find(new Document("capital", oID));
		Iterator<Document> iter = it.iterator();
		Document doc = new Document();

		ArrayList<Localidad> localidades = new ArrayList<Localidad>();

		while (iter.hasNext()) {
			doc = (Document) iter.next();
			Localidad localidad = new Localidad();
			localidad.setNombre(doc.getString("nombre")); // Añadir la población como localidad
			localidades.add(localidad);
		}
		Provincia provincia = new Provincia(capital, localidades);
		provincias.getProvincia().add(provincia);
		// Este marshall sirve para guardar el documento en el XML
		Marshaller jM = jC.createMarshaller();
		jM.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jM.marshal(provincias, new File(RUTA + documentoXML));
	}

	// Método para añadir una provincia al XML usando DOM
	private static void anadirProvinciaDOM(String capital) throws Exception {
		mongoConectarOnline();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		org.w3c.dom.Document doc = builder.parse(new File(RUTA + documentoXML));

		ObjectId oID = existeCapitalMongo(capital);
		FindIterable<Document> it = database.getCollection("poblaciones").find(new Document("capital", oID));
		Iterator<Document> iter = it.iterator();
		Document docMongo = new Document();

		ArrayList<String> localidades = new ArrayList<String>();

		// Iterar a través de los documentos devueltos por la consulta
		while (iter.hasNext()) {
			docMongo = (Document) iter.next();
			// Añadir el nombre de la población a la lista de localidades
			localidades.add(docMongo.getString("nombre"));
		}

		// Obtener el nodo de provincias del documento XML
		Node provincias = doc.getElementsByTagName("provincias").item(0);

		// Crear un nuevo elemento de provincia
		Element nuevaProvincia = doc.createElement("provincia");
		// Establecer el atributo nombre de la nueva provincia
		nuevaProvincia.setAttribute("nombre", capital);

		// Iterar a través de las localidades
		for (String localidad : localidades) {
			// Crear un nuevo elemento de localidad
			Element localidadElement = doc.createElement("localidad");
			// Establecer el contenido de texto del elemento de localidad
			localidadElement.setTextContent(localidad);
			// Añadir el elemento de localidad a la nueva provincia
			nuevaProvincia.appendChild(localidadElement);
		}

		// Añadir la nueva provincia a las provincias
		provincias.appendChild(nuevaProvincia);

		// Crear una nueva fábrica de transformadores y un transformador
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		// Crear una nueva fuente DOM y un resultado de transmisión
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(RUTA + documentoXML));
		// Transformar la fuente en el resultado
		transformer.transform(source, result);
	}
}