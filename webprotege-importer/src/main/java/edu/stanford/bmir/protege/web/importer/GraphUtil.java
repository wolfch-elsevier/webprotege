package edu.stanford.bmir.protege.web.importer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;

public class GraphUtil {

    public GraphUtil() {
    }

    // Convert DataTypeProperty in H-Graph to AnnotationProperty in WP, for these...
    static final String[] dt2at = new String[] { "hasImui", "hasSourceCode", "hasRevRelation" };

    // Convert ObjectProperty in H-Graph to AnnotationPropert in WPy, for these...
    //@formatter:off
    static final String[] op2ap = new String[] {
	    "hasSource",
	    "hasMN",
	    "hasCFN",
	    "hasSyn",
	    "hasKeywordNegative",
	    "hasKeywordPositive",
	    "extBroader",
	    "extExact",
	    "extNarrower",
	    "extNd",
	    "extOverlaps",
	    "hasExtCode",
	    "hasRelatedDocument",
	    "hasExcerpt",
	    "mentionedInExcerpt",
	    "hasAgeRange",
	    "hasCohort",
	    "hasCondition",
	    "hasSex",
	    "hasDomain",
	    "hasEthnicityRace",
	    "hasFrequencyStatistic",
	    "hasGeographicLocalization",
	    "hasLocalizedSpelling",
	    "hasDocument",
	    "hasSourceLocality"
    };
    //@formatter:on

    static final Map<String, String> prefixes = new LinkedHashMap<>();
    static {
	prefixes.put("core", "https://data.elsevier.com/health/core/");
	prefixes.put("coreschema", "https://data.elsevier.com/health/core/schema/");
	prefixes.put("coreterm", "https://data.elsevier.com/health/core/term/");
	prefixes.put("coreconcept", "https://data.elsevier.com/health/core/concept/");
	prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
	prefixes.put("purl", "http://purl.org/dc/elements/1.1/");
	prefixes.put("schema", "http://schema.org/");
	prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
	prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    }

    static final Set<String> t2sco = new HashSet<>();
    static {
	t2sco.add("https://data.elsevier.com/health/core/schema/SemanticType");
	t2sco.add("https://data.elsevier.com/health/core/schema/SemanticGroup");
	t2sco.add("http://schema.org/City");
	t2sco.add("http://schema.org/State");
	t2sco.add("http://schema.org/Country");
    }

    public static Model refineModel(String dataName, Path modelInputFile) throws IOException {
	Model model = ModelFactory.createDefaultModel();
	model.setNsPrefixes(prefixes);
	String urlStr = "file://" + modelInputFile.normalize().toAbsolutePath().toString();
	model.read(urlStr, Lang.N3.getName());

	Resource r;
	Statement s;

	// Convert DataTypeProperty in H-Graph to AnnotationProperty in WebProtege
	for (String name : dt2at) {
	    r = model.getResource(prefixes.get("coreschema") + name);
	    s = r.getRequiredProperty(new PropertyImpl(prefixes.get("rdf") + "type"));
	    s.changeObject(model.createResource(prefixes.get("owl") + "AnnotationProperty"));
	}

	// Convert ObjectProperty in H-Graph to AnnotationProperty in WebProtege
	for (String name : op2ap) {
	    r = model.getResource(prefixes.get("coreschema") + name);
	    s = r.getRequiredProperty(new PropertyImpl(prefixes.get("rdf") + "type"));
	    s.changeObject(model.createResource(prefixes.get("owl") + "AnnotationProperty"));
	}

	// Add missing ObjectProperty Instantiations
	for (String name : new String[] { "isObjectOf", "isSubjectOf" }) {
	    model.add(model.createStatement(model.createResource(prefixes.get("coreschema") + name),
		    new PropertyImpl(prefixes.get("rdf") + "type"),
		    model.createResource(prefixes.get("owl") + "ObjectProperty")));
	}

	// Cannot change model while iterating its' statements, else will
	// throw ConcurrentModificationException, so keep a tally
	// of adds and removes and apply outside of those loops
	List<Statement> removes = new ArrayList<>();
	List<Statement> adds = new ArrayList<>();

	Property rdfType = new PropertyImpl(prefixes.get("rdf") + "type");
	StmtIterator iter = model.listStatements(new SimpleSelector(null, rdfType, (RDFNode) null) {
	    public boolean selects(Statement s) {
		return t2sco.contains(s.getObject().asResource().getURI());
	    }
	});

	while (iter.hasNext()) {
	    Statement stmnt = iter.next();
	    Statement stmnt2 = new StatementImpl(stmnt.getSubject(),
		    model.createProperty(prefixes.get("rdfs"), "subClassOf"), stmnt.getObject());
	    removes.add(stmnt);
	    adds.add(stmnt2);
	}

	// Add OWL.deprecated is True for Concepts that have an RDFS.Comment "to be
	// deprecated"
	Property rdfsComment = new PropertyImpl(prefixes.get("rdfs") + "comment");
	Literal l = model.createLiteral("to be deprecated", "en");
	iter = model.listStatements(new SimpleSelector(null, rdfsComment, l));

	while (iter.hasNext()) {
	    Statement stmnt = iter.next();
	    Statement stmnt2 = new StatementImpl(stmnt.getSubject(),
		    model.createProperty(prefixes.get("owl"), "deprecated"), model.createTypedLiteral(true));
	    adds.add(stmnt2);
	}

	// Fix the EMMeT concept to go under Concept
	Statement stmnt = model.createStatement(
		model.createResource(prefixes.get("coreconcept") + "id-cccc5330-7d23-35e9-a9f5-fc62b59aeeda"),
		new PropertyImpl(prefixes.get("rdfs") + "subClassOf"),
		model.createResource(prefixes.get("coreschema") + "Concept"));
	adds.add(stmnt);

	// Fix the varying external mapping annotation properties to go under a new Ext
	// property
	Resource hasExtMapping = model.createResource(prefixes.get("coreschema") + "hasExtMapping");

	stmnt = model.createStatement(hasExtMapping, new PropertyImpl(prefixes.get("rdf") + "type"),
		model.createResource(prefixes.get("owl") + "AnnotationProperty"));
	adds.add(stmnt);

	stmnt = model.createStatement(hasExtMapping, new PropertyImpl(prefixes.get("rdfs") + "domain"),
		model.createResource(prefixes.get("coreschema") + "Concept"));
	adds.add(stmnt);

	stmnt = model.createStatement(hasExtMapping, new PropertyImpl(prefixes.get("rdfs") + "range"),
		model.createResource(prefixes.get("coreschema") + "ExternalResource"));
	adds.add(stmnt);

	for (String name : new String[] { "extBroader", "extExact", "extNarrower", "extNd", "extOverlaps" }) {
	    stmnt = model.createStatement(model.createResource(prefixes.get("coreschema") + name),
		    new PropertyImpl(prefixes.get("rdfs") + "subPropertyOf"), hasExtMapping);
	    adds.add(stmnt);
	}

	// Fix the varying hasKeyword mapping annotation properties to go under a new
	// "hasKeyword" property
	Resource hasKeyword = model.createResource(prefixes.get("coreschema") + "hasKeyword");

	stmnt = model.createStatement(hasKeyword, new PropertyImpl(prefixes.get("rdf") + "type"),
		model.createResource(prefixes.get("owl") + "AnnotationProperty"));
	adds.add(stmnt);

	stmnt = model.createStatement(hasKeyword, new PropertyImpl(prefixes.get("rdfs") + "domain"),
		model.createResource(prefixes.get("coreschema") + "Term"));
	adds.add(stmnt);

	stmnt = model.createStatement(hasKeyword, new PropertyImpl(prefixes.get("rdfs") + "range"),
		model.createResource(prefixes.get("coreschema") + "Keyword"));
	adds.add(stmnt);

	for (String name : new String[] { "hasKeywordNegative", "hasKeywordPositive" }) {
	    stmnt = model.createStatement(model.createResource(prefixes.get("coreschema") + name),
		    new PropertyImpl(prefixes.get("rdfs") + "subPropertyOf"), hasKeyword);
	    adds.add(stmnt);
	}

	// Add a new ExternalResource "EMMeT" and moving ExternalResource under Source
	Resource emmet = model.createResource(prefixes.get("coreschema") + "EMMeT");

	stmnt = model.createStatement(emmet, new PropertyImpl(prefixes.get("rdfs") + "subClassOf"),
		model.createResource(prefixes.get("coreschema") + "ExternalResource"));
	adds.add(stmnt);

	stmnt = model.createStatement(emmet, new PropertyImpl(prefixes.get("rdfs") + "label"),
		model.createLiteral("EMMeT", "en"));
	adds.add(stmnt);

	stmnt = model.createStatement(emmet, new PropertyImpl(prefixes.get("rdfs") + "comment"),
		model.createLiteral("The EMMeT database which forms the core of H-Graph", "en"));
	adds.add(stmnt);

	stmnt = model.createStatement(model.createResource(prefixes.get("coreschema") + "ExternalResource"),
		new PropertyImpl(prefixes.get("rdfs") + "subClassOf"),
		model.createResource(prefixes.get("coreschema") + "Source"));
	adds.add(stmnt);

	// Add modifications for retired concepts
	Resource hasRetiredImui = model.createResource(prefixes.get("coreschema") + "hasRetiredImui");
	Resource retiredConcept = model.createResource(prefixes.get("coreschema") + "RetiredConcept");

	stmnt = model.createStatement(hasRetiredImui, new PropertyImpl(prefixes.get("rdf") + "type"),
		model.createResource(prefixes.get("owl") + "AnnotationProperty"));
	adds.add(stmnt);

	stmnt = model.createStatement(hasRetiredImui, new PropertyImpl(prefixes.get("rdfs") + "label"),
		model.createLiteral("Retired IMUI", "en"));
	adds.add(stmnt);

	stmnt = model.createStatement(retiredConcept, new PropertyImpl(prefixes.get("rdf") + "type"),
		model.createResource(prefixes.get("owl") + "Class"));
	adds.add(stmnt);

	stmnt = model.createStatement(retiredConcept, new PropertyImpl(prefixes.get("rdfs") + "label"),
		model.createLiteral("Retired Concept", "en"));
	adds.add(stmnt);

	model.remove(removes);
	model.add(adds);

	return model;
    }

    public static void transform(Path src, Lang srcFmt, Path dst, Lang dstFmt) throws IOException {
	StreamRDF destination = StreamRDFWriter.getWriterStream(Files.newOutputStream(dst), dstFmt);
	try {
	    RDFParser.source(src).lang(srcFmt).parse(destination);
	} catch (RiotNotFoundException e) { //this is a dumb exception - it throws away details.
	    String msg = String.format("Cannot open \"%s\" for parsing.", src);
	    throw new IOException(msg, e);
	}
    }

    public static void addRankPadding(Path n3File) throws IOException {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	URI uri;
	try {
	    uri = cl.getResource("rankpadding.n3").toURI();
	} catch (URISyntaxException e) {
	    throw new IOException("Cannot get resource rankpadding.n3", e);
	}
	Files.write(n3File, Files.readAllBytes(Paths.get(uri)), StandardOpenOption.APPEND);
    }

    public static void main(String[] args) throws Exception {
	Model model = refineModel("model", Paths.get("model.n3"));
	model.write(Files.newOutputStream(Paths.get("model_jena.ttl")), Lang.TTL.getName());
    }

}
