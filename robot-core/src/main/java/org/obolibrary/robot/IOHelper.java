package org.obolibrary.robot;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.Context;
import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

/**
 * Provides convenience methods for working with ontology and term files.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class IOHelper {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(IOHelper.class);

    /**
     * RDF literal separator.
     */
    private static String seperator = "\"^^";

    /**
     * Path to default context as a resource.
     */
    private static String defaultContextPath = "/obo_context.jsonld";

    /**
     * Store the current JSON-LD context.
     */
    private Context context = new Context();

    /**
     * Create a new IOHelper with the default prefixes.
     */
    public IOHelper() {
        try {
            setContext(loadContext());
        } catch (IOException e) {
            logger.warn("Could not load default prefixes.");
            logger.warn(e.getMessage());
        }
    }

    /**
     * Create a new IOHelper with or without the default prefixes.
     *
     * @param defaults false if defaults should not be used
     */
    public IOHelper(boolean defaults) {
        try {
            if (defaults) {
                setContext(loadContext());
            } else {
                setContext();
            }
        } catch (IOException e) {
            logger.warn("Could not load default prefixes.");
            logger.warn(e.getMessage());
        }
    }

    /**
     * Create a new IOHelper with the specified prefixes.
     *
     * @param map the prefixes to use
     */
    public IOHelper(Map<String, Object> map) {
        setContext(map);
    }

    /**
     * Create a new IOHelper with prefixes from a file path.
     *
     * @param path to a JSON-LD file with a @context
     */
    public IOHelper(String path) {
        try {
            setContext(loadContext(path));
        } catch (IOException e) {
            logger.warn("Could not load prefixes from " + path);
            logger.warn(e.getMessage());
        }
    }

    /**
     * Create a new IOHelper with prefixes from a file.
     *
     * @param file a JSON-LD file with a @context
     */
    public IOHelper(File file) {
        try {
            setContext(loadContext(file));
        } catch (IOException e) {
            logger.warn("Could not load prefixes from " + file);
            logger.warn(e.getMessage());
        }
    }

    /**
     * Try to guess the location of the catalog.xml file.
     * Looks in the directory of the given ontology file for a catalog file.
     *
     * @param ontologyFile the
     * @return the guessed catalog File; may not exist!
     */
    public File guessCatalogFile(File ontologyFile) {
        String path = ontologyFile.getParent();
        String catalogPath = path + "/catalog-v001.xml";
        return new File(catalogPath);
    }

    /**
     * Load an ontology from a String path, using a catalog file if available.
     *
     * @param ontologyPath the path to the ontology file
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile = guessCatalogFile(ontologyFile);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a String path, with option to use catalog file.
     *
     * @param ontologyPath the path to the ontology file
     * @param useCatalog when true, a catalog file will be used if one is found
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath, boolean useCatalog)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile = null;
        if (useCatalog) {
            catalogFile = guessCatalogFile(ontologyFile);
        }
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a String path, with optional catalog file.
     *
     * @param ontologyPath the path to the ontology file
     * @param catalogPath the path to the catalog file
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(String ontologyPath, String catalogPath)
            throws IOException {
        File ontologyFile = new File(ontologyPath);
        File catalogFile  = new File(catalogPath);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, using a catalog file if available.
     *
     * @param ontologyFile the ontology file to load
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile)
            throws IOException {
        File catalogFile = guessCatalogFile(ontologyFile);
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, with option to use a catalog file.
     *
     * @param ontologyFile the ontology file to load
     * @param useCatalog when true, a catalog file will be used if one is found
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile, boolean useCatalog)
            throws IOException {
        File catalogFile = null;
        if (useCatalog) {
            catalogFile = guessCatalogFile(ontologyFile);
        }
        return loadOntology(ontologyFile, catalogFile);
    }

    /**
     * Load an ontology from a File, with optional catalog File.
     *
     * @param ontologyFile the ontology file to load
     * @param catalogFile the catalog file to use
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(File ontologyFile, File catalogFile)
            throws IOException {
        logger.debug("Loading ontology {} with catalog file {}",
                ontologyFile, catalogFile);

        OWLOntology ontology = null;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            if (catalogFile != null && catalogFile.isFile()) {
                manager.addIRIMapper(new CatalogXmlIRIMapper(catalogFile));
            }
            ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        } catch (OWLOntologyCreationException e) {
            throw new IOException(e);
        }
        return ontology;
    }

    /**
     * Load an ontology from an InputStream, without a catalog file.
     *
     * @param ontologyStream the ontology stream to load
     * @return a new ontology object, with a new OWLManager
     * @throws IOException on any problem
     */
    public OWLOntology loadOntology(InputStream ontologyStream)
            throws IOException {
        OWLOntology ontology = null;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);
        } catch (OWLOntologyCreationException e) {
            throw new IOException(e);
        }
        return ontology;
    }

    /**
     * Given the name of a file format, return an instance of it.
     *
     * Suported formats:
     *
     * <li>OBO as 'obo'
     * <li>RDFXML as 'owl'
     * <li>Turtle as 'ttl'
     * <li>OWLXML as 'owx'
     * <li>Manchester as 'omn'
     * <li>OWL Functional as 'ofn'
     *
     * @param formatName the name of the format
     * @return an instance of the format
     * @throws IllegalArgumentException if format name is not recognized
     */
    public static OWLOntologyFormat getFormat(String formatName)
          throws IllegalArgumentException {
        formatName = formatName.trim().toLowerCase();
        if (formatName.equals("obo")) {
            return new org.coode.owlapi.obo.parser.OBOOntologyFormat();
        } else if (formatName.equals("owl")) {
            return new org.semanticweb.owlapi.io.RDFXMLOntologyFormat();
        } else if (formatName.equals("ttl")) {
            return new org.coode.owlapi.turtle.TurtleOntologyFormat();
        } else if (formatName.equals("owx")) {
            return new org.semanticweb.owlapi.io.OWLXMLOntologyFormat();
        } else if (formatName.equals("omn")) {
            return new org.coode.owlapi.manchesterowlsyntax
                .ManchesterOWLSyntaxOntologyFormat();
        } else if (formatName.equals("ofn")) {
            return new org.semanticweb.owlapi.io
                .OWLFunctionalSyntaxOntologyFormat();
        } else {
            throw new IllegalArgumentException(
                    "Unknown ontology format: " + formatName);
        }
    }

    /**
     * Save an ontology to a String path.
     *
     * @param ontology the ontology to save
     * @param ontologyPath the path to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(OWLOntology ontology, String ontologyPath)
            throws IOException {
        return saveOntology(ontology, new File(ontologyPath));
    }

    /**
     * Save an ontology to a File.
     *
     * @param ontology the ontology to save
     * @param ontologyFile the file to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(OWLOntology ontology, File ontologyFile)
            throws IOException {
        return saveOntology(ontology, IRI.create(ontologyFile));
    }

    /**
     * Save an ontology to an IRI,
     * using the file extension to determine the format.
     *
     * @param ontology the ontology to save
     * @param ontologyIRI the IRI to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology, IRI ontologyIRI)
            throws IOException {
        try {
            String formatName = FilenameUtils.getExtension(
                    ontologyIRI.toString());
            OWLOntologyFormat format = getFormat(formatName);
            return saveOntology(ontology, format, ontologyIRI);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Save an ontology in the given format to a file.
     *
     * @param ontology the ontology to save
     * @param format the ontology format to use
     * @param ontologyFile the file to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology,
            OWLOntologyFormat format, File ontologyFile)
            throws IOException {
        return saveOntology(ontology, format, IRI.create(ontologyFile));
    }

    /**
     * Save an ontology in the given format to an IRI.
     *
     * @param ontology the ontology to save
     * @param format the ontology format to use
     * @param ontologyIRI the IRI to save the ontology to
     * @return the saved ontology
     * @throws IOException on any problem
     */
    public OWLOntology saveOntology(final OWLOntology ontology,
            OWLOntologyFormat format, IRI ontologyIRI)
            throws IOException {
        logger.debug("Saving ontology {} as {} with to IRI {}",
                ontology, format, ontologyIRI);

        if (format instanceof PrefixOWLOntologyFormat) {
            ((PrefixOWLOntologyFormat) format)
                .copyPrefixesFrom(getPrefixManager());
        }

        try {
            ontology.getOWLOntologyManager().saveOntology(
                    ontology, format, ontologyIRI);
        } catch (OWLOntologyStorageException e) {
            throw new IOException(e);
        }
        return ontology;
    }


    /**
     * Extract a set of term identifiers from an input string
     * by removing comments, trimming lines, and removing empty lines.
     * A comment is a space or newline followed by a '#',
     * to the end of the line. This excludes '#' characters in IRIs.
     *
     * @param input the String containing the term identifiers
     * @return a set of term identifier strings
     */
    public Set<String> extractTerms(String input) {
        Set<String> results = new HashSet<String>();
        List<String> lines = Arrays.asList(
                input.replaceAll("\\r", "").split("\\n"));
        for (String line: lines) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            String result = line.replaceFirst("($|\\s)#.*$", "").trim();
            if (!result.isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Given a term string, use the current prefixes to create an IRI.
     *
     * @param term the term to convert to an IRI
     * @return the new IRI
     */
    public IRI createIRI(String term) {
        if (term == null) {
            return null;
        }

        try {
            // This is stupid, because better methods aren't public.
            // We create a new JSON map and add one entry
            // with the term as the key and some string as the value.
            // Then we run the JsonLdApi to expand the JSON map
            // in the current context, and just grab the first key.
            // If everything worked, that key will be our expanded iri.
            Map<String, Object> jsonMap = new HashMap<String, Object>();
            jsonMap.put(term, "ignore this string");
            Object expanded = new JsonLdApi().expand(context, jsonMap);
            String result = ((Map<String, Object>) expanded)
                .keySet().iterator().next();
            if (result != null) {
                return IRI.create(result);
            }
        } catch (Exception e) {
            logger.warn("Could not create IRI for {}", term);
            logger.warn(e.getMessage());
        }
        return null;
    }

    /**
     * Given a set of term identifier strings, return a set of IRIs.
     *
     * @param terms the set of term identifier strings
     * @return the set of IRIs
     * @throws IllegalArgumentException if term identifier is not a valid IRI
     */
    public Set<IRI> createIRIs(Set<String> terms)
            throws IllegalArgumentException {
        Set<IRI> iris = new HashSet<IRI>();
        for (String term: terms) {
            IRI iri = createIRI(term);
            if (iri != null) {
                iris.add(iri);
            }
        }
        return iris;
    }

    /**
     * Given a string that could be an IRI or a literal,
     * return an OWLAnnotationValue.
     * Examples:
     *
     * <li>IRI: <http://example.com>
     * <li>IRI (CURIE): <rdfs:label>
     * <li>plain literal: foo
     * <li>typed literal: "100"^xsd:integer
     *
     * @param value a string that could be an IRI or a literal
     * @return an IRI or OWLLiteral
     */
    public OWLAnnotationValue createValue(String value) {
        if (value.trim().startsWith("<")
            && value.trim().endsWith(">")) {
            return createIRI(value.substring(1, value.length() - 1));
        }

        return createLiteral(value);
    }

    /**
     * Given a value string that might include type information,
     * return an OWLLiteral.
     * If the input is: "content"^^xsd:type
     *
     * @param value the string to parse
     * @return the string for the lexical value
     */
    public OWLLiteral createLiteral(String value) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();

        String lexicalValue = getLexicalValue(value);
        String valueType = getValueType(value);

        IRI datatypeIRI = createIRI(valueType);

        OWLDatatype datatype = null;
        if (datatypeIRI != null) {
            datatype = df.getOWLDatatype(datatypeIRI);
        }

        if (datatype == null) {
            return df.getOWLLiteral(lexicalValue);
        } else {
            return df.getOWLLiteral(lexicalValue, datatype);
        }
    }

    /**
     * Given a value string that might include type information,
     * return the lexical value.
     * If the input is: "content"^^xsd:type
     * then the result is: content
     *
     * @param value the string to parse
     * @return the string for the lexical value
     */
    public static String getLexicalValue(String value) {
        if (!value.startsWith("\"")) {
            return value;
        }
        if (value.indexOf(seperator) == -1) {
            return value;
        }
        return value.substring(1, value.lastIndexOf(seperator));
    }

    /**
     * Given a value string that might include type information,
     * return the type IRI/CURIE string, or null.
     * If the format is: "content"^^xsd:type
     * then the result is: xsd:type
     *
     * @param value the value to get the type of
     * @return the string for the type
     */
    public static String getValueType(String value) {
        value = value.trim();
        if (!value.startsWith("\"")) {
            return null;
        }
        if (value.indexOf(seperator) == -1) {
            return null;
        }
        return value.substring(value.lastIndexOf(seperator)
                    + seperator.length());
    }

    /**
     * Parse a set of IRIs from a space-separated string, ignoring '#' comments.
     *
     * @param input the string containing the IRI strings
     * @return the set of IRIs
     * @throws IllegalArgumentException if term identifier is not a valid IRI
     */
    public Set<IRI> parseTerms(String input) throws IllegalArgumentException {
        return createIRIs(extractTerms(input));
    }

    /**
     * Load a set of IRIs from a file.
     *
     * @param path the path to the file containing the terms
     * @return the set of IRIs
     * @throws IOException on any problem
     */
    public Set<IRI> loadTerms(String path) throws IOException {
        return loadTerms(new File(path));
    }

    /**
     * Load a set of IRIs from a file.
     *
     * @param file the File containing the terms
     * @return the set of IRIs
     * @throws IOException on any problem
     */
    public Set<IRI> loadTerms(File file) throws IOException {
        String content = new Scanner(file).useDelimiter("\\Z").next();
        return parseTerms(content);
    }

    /**
     * Load a map of prefixes from "@context" of the default JSON-LD file.
     *
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Context loadContext() throws IOException {
        return loadContext(
                IOHelper.class.getResourceAsStream(defaultContextPath));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD file
     * at the given path.
     *
     * @param path the path to the JSON-LD file
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Context loadContext(String path) throws IOException {
        return loadContext(new File(path));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD file.
     *
     * @param file the JSON-LD file
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Context loadContext(File file) throws IOException {
        return loadContext(new FileInputStream(file));
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD InputStream.
     *
     * @param stream the JSON-LD content as an InputStream
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Context loadContext(InputStream stream) throws IOException {
        String content = new Scanner(stream).useDelimiter("\\Z").next();
        return parseContext(content);
    }

    /**
     * Load a map of prefixes from the "@context" of a JSON-LD string.
     *
     * @param jsonString the JSON-LD string
     * @return a map from prefix name strings to prefix IRI strings
     * @throws IOException on any problem
     */
    public static Context parseContext(String jsonString) throws IOException {
        try {
            Object jsonObject = JsonUtils.fromString(jsonString);
            if (!(jsonObject instanceof Map)) {
                return null;
            }
            Map<String, Object> jsonMap = (Map<String, Object>) jsonObject;
            if (!jsonMap.containsKey("@context")) {
                return null;
            }
            Object jsonContext = jsonMap.get("@context");
            return new Context().parse(jsonContext);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Get a copy of the current context.
     *
     * @return a copy of the current context
     */
    public Context getContext() {
        return this.context.clone();
    }

    /**
     * Set an empty context.
     */
    public void setContext() {
        this.context = new Context();
    }

    /**
     * Set the current JSON-LD context to the given context.
     *
     * @param context the new JSON-LD context
     */
    public void setContext(Context context) {
        if (context == null) {
            setContext();
        } else {
            this.context = context;
        }
    }

    /**
     * Set the current JSON-LD context to the given map.
     *
     * @param map a map of strings for the new JSON-LD context
     */
    public void setContext(Map<String, Object> map) {
        try {
            this.context = new Context().parse(map);
        } catch (Exception e) {
            logger.warn("Could not set context {}", map);
            logger.warn(e.getMessage());
        }
    }

    /**
     * Make an OWLAPI PrefixManager from a map of prefixes.
     *
     * @param prefixes a map from prefix name strings to prefix IRI strings
     * @return a PrefixManager
     */
    public static PrefixManager makePrefixManager(
            Map<String, String> prefixes) {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        for (Map.Entry<String, String> entry: prefixes.entrySet()) {
            pm.setPrefix(entry.getKey() + ":", entry.getValue());
        }
        return pm;
    }

    /**
     * Load an OWLAPI PrefixManager from the default JSON-LD file.
     *
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager() throws IOException {
        return makePrefixManager(loadContext().getPrefixes(false));
    }

    /**
     * Load an OWLAPI PrefixManager from the given JSON-LD file path.
     *
     * @param path to the JSON-LD file
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager(String path)
            throws IOException {
        return makePrefixManager(loadContext(path).getPrefixes(false));
    }

    /**
     * Load an OWLAPI PrefixManager from the given JSON-LD file.
     *
     * @param file the JSON-LD file
     * @return a PrefixManager
     * @throws IOException on any problem
     */
    public static PrefixManager loadPrefixManager(File file)
            throws IOException {
        return makePrefixManager(loadContext(file).getPrefixes(false));
    }

    /**
     * Get a prefix manager with the current prefixes.
     *
     * @return a new PrefixManager
     */
    public PrefixManager getPrefixManager() {
        return makePrefixManager(context.getPrefixes(false));
    }

    /**
     * Add a prefix mapping as a single string "foo: http://example.com#".
     *
     * @param combined both prefix and target
     * @throws IllegalArgumentException on malformed input
     */
    public void addPrefix(String combined) throws IllegalArgumentException {
        String[] results = combined.split(":", 2);
        if (results.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid prefix string: " + combined);
        }
        addPrefix(results[0], results[1]);
    }

    /**
     * Add a prefix mapping to the current JSON-LD context,
     * as a prefix string and target string.
     * Rebuilds the context.
     *
     * @param prefix the short prefix to add; should not include ":"
     * @param target the IRI string that is the target of the prefix
     */
    public void addPrefix(String prefix, String target) {
        try {
            context.put(prefix.trim(), target.trim());
            context.remove("@base");
            setContext((Map<String, Object>) context);
        } catch (Exception e) {
            logger.warn("Could not load add prefix \"{}\" \"{}\"",
                    prefix, target);
            logger.warn(e.getMessage());
        }
    }

    /**
     * Get a copy of the current prefix map.
     *
     * @return a copy of the current prefix map
     */
    public Map<String, String> getPrefixes() {
        return this.context.getPrefixes(false);
    }

    /**
     * Set the current prefix map.
     *
     * @param map the new map of prefixes to use
     */
    public void setPrefixes(Map<String, Object> map) {
        setContext(map);
    }

    /**
     * Return the current prefixes as a JSON-LD string.
     *
     * @return the current prefixes as a JSON-LD string
     * @throws IOException on any error
     */
    public String getContextString() throws IOException {
        try {
            Object compact = JsonLdProcessor.compact(
                    JsonUtils.fromString("{}"),
                    context.getPrefixes(false),
                    new JsonLdOptions());
            return JsonUtils.toPrettyString(compact);
        } catch (Exception e) {
            throw new IOException("JSON-LD could not be generated", e);
        }
    }

    /**
     * Write the current context as a JSON-LD file.
     *
     * @param path the path to write the context
     * @throws IOException on any error
     */
    public void saveContext(String path) throws IOException {
        saveContext(new File(path));
    }

    /**
     * Write the current context as a JSON-LD file.
     *
     * @param file the file to write the context
     * @throws IOException on any error
     */
    public void saveContext(File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(getContextString());
        writer.close();
    }

}