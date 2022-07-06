package edu.stanford.bmir.protege.web.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WPHttpClient implements AutoCloseable {
    protected static final Logger logger = LoggerFactory.getLogger(WPHttpClient.class);
    // because org.apache.http.entity.ContentType.APPLICATION_XML is charset:
    // ISO_8859_1
    public static final ContentType APPLICATION_XML_UTF8 = ContentType.create("application/xml", Consts.UTF_8);

    // because DEFAULT_TEXT is also ISO_8859_1
    public static final ContentType TEXT_PLAIN_UTF8 = ContentType.create("text/plain", Consts.UTF_8);

    protected final CloseableHttpClient httpClient;
    protected String authHeader;

    public WPHttpClient() throws IOException {
	this(20);
    }

    public WPHttpClient(int httpTimeoutSeconds) {
	this(httpTimeoutSeconds, null, null);
    }

    public WPHttpClient(int httpTimeoutSeconds, String username, String password) {
	if (!StringUtils.isBlank(username)) {
	    byte[] creds;
	    try {
		creds = (username + ":" + password).getBytes("UTF-8");
	    } catch (UnsupportedEncodingException e) {
		String msg = String.format("Cannot encode user/pass for username \"%s\"", username);
		throw new RuntimeException(msg, e);
	    }
	    authHeader = "Basic " + Base64.getEncoder().encodeToString(creds);
	}

	int requestTimeoutSeconds = httpTimeoutSeconds * 3;
      //@formatter:off
      RequestConfig config = RequestConfig.custom()
          .setConnectTimeout(httpTimeoutSeconds * 1000)
          .setConnectionRequestTimeout(requestTimeoutSeconds * 1000)
          .setSocketTimeout(requestTimeoutSeconds * 1000)
          .setAuthenticationEnabled(true)
          .setCookieSpec(CookieSpecs.STANDARD)
          .build();

      httpClient = HttpClientBuilder.create()
	  .setUserAgent("webprotege-importer/version-5")
          .setDefaultRequestConfig(config)
          .setRetryHandler(new DefaultHttpRequestRetryHandler())
          .build();
      //@formatter:on
    }

    public void postForm(String url, Map<String, Object> formParams, String acceptHdr, Path resultFile) {
	HttpPost httpPost = new HttpPost(url);
	httpPost.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
	httpPost.addHeader(HttpHeaders.ACCEPT, acceptHdr); // "application/n-triples;charset=UTF-8");
	httpPost.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");

	// CloseableHttpResponse response = null;
	List<NameValuePair> httpParams = convertToHttpParams(formParams);

	try {
	    httpPost.setEntity(new UrlEncodedFormEntity(httpParams));
	    // response = httpClient.execute(httpPost);
	} catch (Exception e) {
	    throw new WPHttpException(e);
	}

	try (CloseableHttpResponse response = httpClient.execute(httpPost);) {

	    if (response.getStatusLine().getStatusCode() != 200)
		throw new WPHttpException(response.getStatusLine().toString());

	    HttpEntity entity = response.getEntity();

	    logger.info("POST response: content-legnth: {}, isChuncked: {}. isStreaming: {}", entity.getContentLength(),
		    entity.isChunked(), entity.isStreaming());

	    try (BufferedReader br = new BufferedReader(
		    new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8.name()));
		    BufferedWriter bw = Files.newBufferedWriter(resultFile, StandardCharsets.UTF_8);) {
		String line;

		while ((line = br.readLine()) != null) {
		    bw.write(line + "\n");
		}
	    }
	} catch (IOException e) {
	    throw new WPHttpException("Error processing POST to " + url, e);
	}
    }

    protected List<NameValuePair> convertToHttpParams(Map<String, Object> params) {
	// convert Map<String, String> params to n-v pair list
	// Not worrying about multivalues, for now

	//@formatter:off
	return params.entrySet().stream()
            .map(entry -> new BasicNameValuePair(entry.getKey(), (String)entry.getValue()))
	    .collect(Collectors.toCollection(ArrayList::new));
	//@formatter:on
    }

    public URI getURI(String baseURL, String path, Map<String, Object> params) throws URISyntaxException {
	URI uri = null;

	if (StringUtils.isBlank(path))
	    uri = new URI(baseURL);
	else
	    uri = new URI(baseURL + "/" + path);

	URIBuilder builder = new URIBuilder(baseURL);

	// Not worrying about multivalues, for now,
	// but that's why value is of type Object, not String
	if (params != null && params.size() > 0) {
	    for (Map.Entry<String, Object> entry : params.entrySet()) {
		builder.setParameter(entry.getKey(), (String) entry.getValue());
	    }
	}

	uri = builder.build();

	return uri;
    }

    @Override
    public void close() throws Exception {
	httpClient.close();
    }

    public CloseableHttpClient getHttpClient() {
	return httpClient;
    }

    public String getAuthHeader() {
	return authHeader;
    }

    public void setAuthHeader(String authHeader) {
	this.authHeader = authHeader;
    }
}
