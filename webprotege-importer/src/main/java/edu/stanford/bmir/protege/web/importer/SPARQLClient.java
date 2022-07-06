package edu.stanford.bmir.protege.web.importer;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import edu.stanford.bmir.protege.web.http.WPHttpClient;

public class SPARQLClient {
    final String hGraphBaseURL;
    final String hGraphUsername;
    final String hGraphPassword;
    static final Logger logger = LoggerFactory.getLogger(SPARQLClient.class);

    public SPARQLClient(final String hGraphBaseURL, final String hGraphUsername, final String hGraphPassword) {
	this.hGraphBaseURL = hGraphBaseURL;
	this.hGraphUsername = hGraphUsername;
	this.hGraphPassword = hGraphPassword;
    }

    public void queryToFile(Path queryFile, String uuid, Path resultFile) throws Exception {
	Stopwatch stopwatch = Stopwatch.createStarted();
	String queryStr = new String(Files.readAllBytes(queryFile), StandardCharsets.UTF_8);
	queryToFile(queryStr, uuid, resultFile);
	stopwatch.stop();
	logger.info("Finished SPARQL query {} in {} seconds", queryFile.getFileName(), stopwatch.elapsed(SECONDS));
    }

    public void queryToFile(String queryStr, String uuid, Path resultFile) throws Exception {
	Map<String, Object> params = new HashMap<>();
	params.put("query", queryStr);
	params.put("format", "n3");
	params.put("output", "n3");
	params.put("results", "n3");

	// TODO: keep and reuse WPHttpClient ?
	try (WPHttpClient httpClient = new WPHttpClient(30, this.hGraphUsername, this.hGraphPassword);) {
	    httpClient.postForm(String.format("%s/repositories/%s", this.hGraphBaseURL, uuid), params,
		    "application/n-triples;charset=UTF-8", resultFile);
	}
    }

}
