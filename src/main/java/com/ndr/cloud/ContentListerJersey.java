package com.ndr.cloud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;

import static johnson.Serializers.listReader;
import static johnson.Serializers.propertyReader;
import static com.ndr.cloud.AwsRequestSigner.signRequest;

public class ContentListerJersey implements ContentLister {

	private static final String awsHost = CredentialsFactory.get().getAWSHost();

	private static final Logger logger = Logger.getLogger("ContentListerJersey");

	/* Verbosity determines how detailed message will be logged.
	 *  The lowest verbosity (HEADERS_ONLY) logs only request/response headers.
	 *  The medium verbosity (PAYLOAD_TEXT) logs request/response headers, as well as an entity if considered a readable text.
	 *  The highest verbosity (PAYLOAD_ANY) logs all types of an entity (besides the request/response headers).
	 *
	 * Note that the entity is logged up to the maximum number specified in any of the following constructors
	 * LoggingFeature.LoggingFeature(Logger, Integer), LoggingFeature.LoggingFeature(Logger, Level, Verbosity, Integer)
	 * or by some of the feature's properties (see LOGGING_FEATURE_MAX_ENTITY_SIZE, LOGGING_FEATURE_MAX_ENTITY_SIZE_CLIENT,
	 * LOGGING_FEATURE_MAX_ENTITY_SIZE_SERVER.
	 */

	private static final Feature feature = new LoggingFeature(logger, Level.INFO, Verbosity.PAYLOAD_ANY, null);

		///////////////////////////////////////////////////////
		// Build an awsRequest from the incoming requestBody
		///////////////////////////////////////////////////////

		// requestBody ==
		//	{
		//		"host": "8qcqoe2z10.execute-api.us-east-1.amazonaws.com",
		//		"path": "/dev/ContentLists/",
		//		"method": "POST",
		//		"body": "{\"name\":\"CL1000\",\"description\":\"New Content List\",\"items\":[{}]}",
		//		"headers": {
		//			"content-type": "application\/json"
		//		}
		//	}

	@Override
	public List<Map<String,Object>> getContentLists(final String customerCode) {

		final Client client = ClientBuilder.newBuilder()
			.register(feature)
			.build();

		final String path = "/dev/ContentLists/";
		final String httpMethod = "GET";
		final Map<String,Object> requestBody = propertyReader(
			"host", awsHost,
			"path", path,
			"method", httpMethod
		);

		try {
			final Map<String,Object> signedRequest = signRequest(requestBody, customerCode);
			final Map<String,String> signedHeaders = (Map<String, String>) signedRequest.get("headers");

			final String absoluteURL = String.format( "https://%s%s", awsHost, signedRequest.get("path") );
			final WebTarget target = client.target( absoluteURL );
			final Invocation.Builder builder = target.request();
			if( signedHeaders!=null ) {
				for (final Map.Entry<String,String> entry : signedHeaders.entrySet() ) {
					final String key = entry.getKey();
					if (!"host".equalsIgnoreCase(key)) {
						// org.glassfish.jersey.client.internal.HttpUrlConnector.restrictedHeaders
						builder.header( key, entry.getValue() );     // -Dsun.net.http.allowRestrictedHeaders=true
					}
				}
			}

			final Response response = builder.build(httpMethod).invoke();

			final int status = response.getStatus();
			System.out.println( "Status code: " + status );
			final List<Map<String,Object>> contentLists = response.readEntity( List.class );
			return contentLists;
		} catch (final IOException | URISyntaxException e) {
			throw new RuntimeException( "While retrieving content lists for "+customerCode, e );
		}
	}

	//List<Map<String,Object>> getContentListInfo(int id);

	//int upsertContentList(String customerCode, String contentListTitle, String contentListDescription, Integer contentListId);

	//void deleteContentList(String customerCode, int contentListId);

	//int upsertContentListItem(String customerCode, int bucketId, int itemId, String comment, Integer ordinal, Integer bucketItemsId);

	//void deleteContentListItem(String customerCode, int bucketItemId);

}
