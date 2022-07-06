package edu.stanford.bmir.protege.web.importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.bmir.protege.web.server.revision.RevisionStoreImpl;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

public class HGraphImporter {

    static final Pattern TTLFILENAME = Pattern.compile("\\w+_(\\d+)\\.ttl");
    static final Logger logger = LoggerFactory.getLogger(HGraphImporter.class);
    final AtomicInteger revisionSubmissions = new AtomicInteger();
    final WPImporter wpImporter;
    final ArgsParser argsParser;
    final ArgsParser.ProgramArgs progArgs;
    final String hGraphBaseURL;
    final String hGraphUsername;
    final String hGraphPassword;
    final String webprotegeApiKey;
    final SPARQLClient sparqlClient;
    final String outputBaseDir;
    final Path cacheDir;
    final boolean isSample;
    final FastDateFormat fileDateFmt;

    public HGraphImporter(String[] args) throws IOException {
	argsParser = new ArgsParser();
	progArgs = argsParser.parseCommandLine(args);
	wpImporter = new WPImporter(progArgs.getString("userid"), progArgs.getString("projectid"),
		Paths.get(progArgs.getString("output_data_dir")));
	fileDateFmt = FastDateFormat.getInstance("YYYYMMdd-HHmmss");

	webprotegeApiKey = readFile(".", "api_key");
	String credStr = readFile(".", "HGRAPH_CRED").trim();
	String[] pieces = credStr.split(":");
	hGraphBaseURL = String.format("%s://%s", pieces[0], pieces[1]);
	hGraphUsername = pieces[2];
	hGraphPassword = pieces[3];
	sparqlClient = new SPARQLClient(hGraphBaseURL, hGraphUsername, hGraphPassword);
	outputBaseDir = progArgs.getString("output_data_dir");
	isSample = "sample".equalsIgnoreCase(progArgs.getString("kind"));

	cacheDir = Paths.get(outputBaseDir, "cache");
	// FileUtils.deleteDirectory(cacheDir.toFile());
	cacheDir.toFile().mkdirs();

	String dataDirTempl = String.format("%s/data-store/project-data/%s/change-data/", outputBaseDir,
		progArgs.getString("projectid"));

	Path revsionsFileDir = Paths.get(dataDirTempl);
	if (revsionsFileDir.toFile().isDirectory()) {
	    Path revisionsFile = Paths.get(dataDirTempl, "change-data.binary");
	    if (Files.exists(revisionsFile)) {
		PosixFileAttributes attrs = Files.readAttributes(revisionsFile, PosixFileAttributes.class);
		FileTime ft = attrs.lastModifiedTime();
		String timestamp = fileDateFmt.format(ft.toMillis());
		Files.move(revisionsFile, revisionsFile.resolveSibling("change-data.binary" + "_" + timestamp));
	    }
	} else {
	    revsionsFileDir.toFile().mkdirs();
	}
    }

    public static void main(String[] args) throws Exception {
	HGraphImporter hGraphImporter = new HGraphImporter(args);
	hGraphImporter.processETL();
    }

    void processETL() throws Exception {
	List<String> parts = (progArgs.getStringList("parts").size() == 1
		&& "all".equalsIgnoreCase(progArgs.getStringList("parts").get(0)))
			? new ArrayList<String>(ArgsParser.validParts)
			: progArgs.getStringList("parts");

	String uuid = null;
	if (progArgs.containsKey("uuid"))
	    uuid = progArgs.getString("uuid");
	else
	    uuid = readFile(".", "uuid.txt");

	String etlPhases = progArgs.getString("phases").toUpperCase();

	// Perform ETL phases
	if (etlPhases.contains("E"))
	    queryHGraph(parts, uuid, cacheDir);

	if (etlPhases.contains("T"))
	    transformQueryResults(parts, cacheDir);

	if (etlPhases.contains("L")) {
	    // importToWebprotege(parts, cacheDir);
	    ProjectId projectId = ProjectId.valueOf(progArgs.getString("projectid"));
	    this.loadCachedTriples(projectId, cacheDir, Paths.get(this.outputBaseDir), parts);
	}
    }

    void queryHGraph(List<String> parts, String uuid, Path cacheDir) throws Exception {
	boolean isSample = "sample".equalsIgnoreCase(progArgs.getString("kind"));

	for (String data_name : parts) {
	    logger.info("Processing {}", data_name);
	    String queryFilename = isSample ? data_name + "_sample.sparql" : data_name + ".sparql";
	    String resultFilename = isSample ? data_name + "_sample.n3" : data_name + ".n3";

	    Path resultFile = Paths.get(outputBaseDir, resultFilename);
	    sparqlClient.queryToFile(Paths.get(progArgs.getString("queries_dir"), queryFilename), uuid, resultFile);
	}
    }

    void transformQueryResults(List<String> parts, Path cacheDir) throws IOException {
	boolean isSample = "sample".equalsIgnoreCase(progArgs.getString("kind"));

	for (String data_name : parts) {
	    String resultFilename = isSample ? data_name + "_sample.n3" : data_name + ".n3";
	    Path resultFile = Paths.get(outputBaseDir, resultFilename);

	    String resultTTLFilename = isSample ? data_name + "_sample.ttl" : data_name + ".ttl";
	    Path resultTTLFile = Paths.get(cacheDir.toString(), resultTTLFilename);

	    switch (data_name) {
	    case "model":
		Model model = GraphUtil.refineModel(data_name, resultFile);
		model.write(Files.newOutputStream(resultTTLFile), Lang.TTL.getName());
		if (!model.isClosed())
		    model.close();
		break;
	    case "relations":
	    case "relations_co":
	    case "relations_healthline":
		GraphUtil.addRankPadding(resultFile, logger);
		GraphUtil.transform(resultFile, Lang.N3, resultTTLFile, Lang.TTL, logger);
		break;
	    default:
		GraphUtil.transform(resultFile, Lang.N3, resultTTLFile, Lang.TTL, logger);
		break;
	    }
	}
    }

    void importToWebprotege(List<String> parts, Path cacheDir) throws IOException {
	ProjectId projectId = ProjectId.valueOf(progArgs.getString("projectid"));
	long revisionNumber = 0;

	RevisionStoreImpl revisionStore = this.wpImporter.openRevisionStore(projectId, Paths.get(this.outputBaseDir));
	revisionNumber = revisionStore.getCurrentRevisionNumber().getValue() + 1;

	for (String data_name : parts) {
	    String cacheFilename = isSample ? data_name + "_sample.ttl" : data_name + ".ttl";
	    Path cacheFile = Paths.get(cacheDir.toString(), cacheFilename);
	    InputStream is = Files.newInputStream(cacheFile);
	    wpImporter.loadTriples(projectId, is, data_name, revisionNumber, revisionStore);
	    revisionNumber++;
	}
    }

    public List<Path> findTTLFiles(Path path) throws IOException {

	List<Path> result;
	try (Stream<Path> pathStream = Files.find(path, Integer.MAX_VALUE,
		(p, basicFileAttributes) -> TTLFILENAME.matcher(p.getFileName().toString()).matches())) {
	    result = pathStream.collect(Collectors.toList());
	}

	Collections.sort(result, new Comparator<Path>() {
	    @Override
	    public int compare(Path o1, Path o2) {
		String f1 = o1.getFileName().toString();
		String f2 = o2.getFileName().toString();
		Matcher m1 = TTLFILENAME.matcher(f1);
		if (!m1.matches())
		    throw new RuntimeException("Unexpected filename for comparison: " + o1.toString());
		Matcher m2 = TTLFILENAME.matcher(f2);
		if (!m2.matches())
		    throw new RuntimeException("Unexpected filename for comparison: " + o2.toString());
		Integer o1Seq = Integer.parseInt(m1.group(1));
		Integer o2Seq = Integer.parseInt(m2.group(1));
		return o1Seq.compareTo(o2Seq);
	    }
	});
	return result;
    }

    void loadCachedTriples(ProjectId projectId, Path cacheDir, Path wpDataDir, List<String> triplesKinds)
	    throws IOException {
	RevisionStoreImpl revisionStore = wpImporter.openRevisionStore(projectId, wpDataDir);
	revisionStore.setSavedHook(new SavedHook(this.wpImporter));
	long revisionNumber = 1L;
	for (String triplesKind : triplesKinds) {
	    List<Path> ttlFiles = findTTLFiles(Paths.get(cacheDir.toString(), triplesKind));
	    logger.info("Loading {} turtle files for {}", ttlFiles.size(), triplesKind);
	    for (Path ttlFile : ttlFiles) {
		String revisionMessage = ttlFile.getFileName().toString().split("\\.")[0];
		wpImporter.loadTriples(projectId, Files.newInputStream(ttlFile), revisionMessage, revisionNumber,
			revisionStore);
		revisionNumber++;
	    }
	}
	revisionStore.dispose();
    }

    String readFile(String baseDir, String filename) throws IOException {
	String data = new String(Files.readAllBytes(Paths.get(baseDir, filename)), StandardCharsets.UTF_8);
	return data.trim();
    }

    static class SavedHook implements Runnable {
	final WPImporter wpImporter;

	public SavedHook(final WPImporter wpImporter) {
	    this.wpImporter = wpImporter;
	}

	@Override
	public void run() {
	    wpImporter.getRevisionSubmissions().decrementAndGet();
	}
    }

}
