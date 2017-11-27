package ca.on.oicr.gsi.shesmu.actions.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.MetaInfServices;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.on.oicr.gsi.shesmu.ActionDefinition;
import ca.on.oicr.gsi.shesmu.ActionRepository;
import ca.on.oicr.gsi.shesmu.Pair;

@MetaInfServices
public final class RemoteActionRepository implements ActionRepository {

	private static CloseableHttpClient httpclient = HttpClients.createDefault();

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Pattern SEMICOLON = Pattern.compile(";");

	private static final String URL_VARIABLE = "SHESMU_ACTION_URLS";

	@Override
	public Stream<Pair<String, Map<String, String>>> listConfiguration() {
		final Map<String, String> map = new TreeMap<>();
		map.put("url", System.getenv(URL_VARIABLE));
		return Stream.of(new Pair<>("Remote Action Repositories", map));
	}

	@Override
	public Stream<ActionDefinition> query() {
		return roots().flatMap(this::queryActionsCatalog);
	}

	private Stream<ActionDefinition> queryActionsCatalog(String url) {
		try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url + "/actioncatalog"))) {
			return Arrays.stream(mapper.readValue(response.getEntity().getContent(), Definition[].class))
					.map(def -> def.toDefinition(url));
		} catch (final ClientProtocolException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return Stream.empty();
	}

	private Stream<String> roots() {
		return Optional.ofNullable(System.getenv(URL_VARIABLE)).map(SEMICOLON::splitAsStream).orElse(Stream.empty());
	}

}
