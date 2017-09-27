package johnson.test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;


import static org.junit.Assert.assertEquals;

import static johnson.Serializers.jDate;
import static johnson.Serializers.formatDate;
import static johnson.Serializers.formatDateJsonRpc;
import static johnson.Serializers.parseDateJsonRpc;
import static johnson.Serializers.write;

/**
 * This test parses "date{...}" to either java.util.Date, SimpleDate,
 * or MillisecondDate, depending on the specified JSONSerializer.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSerializers {

	//--------------------------------------------------

	@Rule
	public TestName testName = new TestName();

    @Before
    public void before() {
		System.out.printf( "\nTesting %s()... %s\n", testName.getMethodName(), formatDate( "yyyy-MM-dd hh:mm:ss a", new Date() ) );
    }

    @After
    public void after() {
		System.out.printf( "...tested %s().\n", testName.getMethodName() );
    }

	//--------------------------------------------------

    List<?> listReader (final Object...objs) {
    	return (List<?>) Arrays.asList( objs );
    }

    Map<String,?> propertyReader (final Object...objects) {
		final Map<String,Object> map = new LinkedHashMap<>();
		for(int i=0; i<objects.length; i+=2) {
			if( (objects[i] instanceof String) ||
				(objects[i] instanceof Enum ) ) {
				map.put( objects[i].toString(), objects[i+1] );
			} else {
				throw new IllegalArgumentException(	"expected String or Enum, but got " + objects[i]);
			}
		}
		return map;
    }

    //--------------------------------------------------

	/**
	 * Parse custom "date{...}" strings.
	 */
	@Test
	public void testDateParser() throws ParseException {

		doTest( jDate( "2000-01-01"),
				parseDateJsonRpc( "\"date{2000-01-01}\"" ) );

		doTest( jDate( "2000-01-1"),
				parseDateJsonRpc( "\"date{2000-01-1}\"" ) );

		doTest( jDate( "2000-1-01"),
				parseDateJsonRpc( "\"date{2000-1-01}\"" ) );

		doTest( jDate( "2000-1-1"),
				parseDateJsonRpc( "\"date{2000-1-1}\"" ) );

		doTest( jDate( "2000-12-1"),
				parseDateJsonRpc( "\"date{2000-12-1}\"" ) );

		doTest( jDate( "2000-1-31"),
				parseDateJsonRpc( "\"date{2000-1-31}\"" ) );

		doTest( jDate( "2000-01-01"),
				parseDateJsonRpc( "\"date{2000-1-1}\"" ) );


		// Support ISO 8601 - https://ndrbugz.atlassian.net/browse/ZEN-1072

		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis( 1439673810803L ); //System.out.println( parseDateJsonRpc( "\"2015-08-15T17:23:30.803Z" ).getTime() );
		doTest( cal.getTime(),
				parseDateJsonRpc( "\"2015-08-15T17:23:30.803Z\"" ) );

		cal.setTimeInMillis( 1440275282507L ); //System.out.println( parseDateJsonRpc( "\"2015-08-22T16:28:02.507Z" ).getTime() );
		doTest( cal.getTime(),
				parseDateJsonRpc( "\"2015-08-22T16:28:02.507Z\"" ) );

		cal.setTimeInMillis( 339408000000L ); //System.out.println( parseDateJsonRpc( "\"1980-10-03T04:00:00.000Z" ).getTime() );
		doTest( cal.getTime(),
				parseDateJsonRpc( "\"1980-10-03T04:00:00.000Z\"" ) );

		cal.setTimeInMillis( 1410339600000L ); //System.out.println( parseDateJsonRpc( "\"2014-09-10T05:00:00.000Z" ).getTime() );
		doTest( cal.getTime(),
				parseDateJsonRpc( "\"2014-09-10T05:00:00.000Z\"" ) );
	}


	//--------------------------------------------------

	/**
	 * Generate "date{...}" strings, then parse them back into
	 * java.util.Date, SimpleDate, and MillisecondDate.
	 */
	@Test
	public void testDateParserEndToEnd() throws ParseException {

		// Convert a java.util.Date to String
		final java.util.Date nowJavaUtilDate = new java.util.Date();
		final String nowJavaUtilDateString = "\"" + formatDateJsonRpc( nowJavaUtilDate ) + "\"";

		// Convert Strings back into original form
		final java.util.Date nowJavaUtilDate2 = parseDateJsonRpc( nowJavaUtilDateString );
		doTest( nowJavaUtilDate, nowJavaUtilDate2 );

	}

	//--------------------------------------------------

	/**
	 * Write arbitrary Java objects into JSON strings
	 */
	@Test
	public void testJsonWriter() throws IOException {

		// Numbers

		doTest( "3.5", write( 3.5 ) );

		doTest( "2.4E16", write( 2.4*Math.pow(10,16) ) );

		doTest( "3.6E-19", write( 3.6*Math.pow(10,-19) ) );

		doTest( "2.56122757695612E-15", write( 2.56122757695612e-15 ) );

		doTest(
			"{\"stdError\":3.14E20}",
			write( propertyReader( "stdError", 3.14e20 ) ) );

		doTest(
			"{\"estimate\":-20,\"stdError\":1.618E99,\"id\":3}",
			write( propertyReader(
					"estimate", -20,
					"stdError", 1.618e99,
					"id", 3 ) )	);


		// Dates

		// "date{2014-08-08-15-49-44-112}"
		final java.util.Date nowJava = new java.util.Date();
		final String jsonNowJava = "\"" + formatDateJsonRpc( nowJava ) + "\"";
		doTest( jsonNowJava, write( nowJava ) );

		// Lists

		doTest( "[]", write( Collections.emptyList() ) );

		doTest( "[\"a\",1,true]", write( listReader( "a", 1, true ) ) );

		doTest( "[\"a\",1,[\"a\",1,[\"a\",1,true]]]",
				write( listReader(
					"a", 1, listReader(
						"a", 1, listReader( "a", 1, true ) ) ) ) );


		// Maps

		doTest( "{}", write( propertyReader() ) );

		doTest( "{\"a\":1,\"b\":true}",
				write( propertyReader(
						"a", 1,
						"b", true ) ) );

		doTest( "{\"a\":1,\"b\":{\"a\":1,\"b\":[\"a\",1,true]}}",
				write( propertyReader(
					"a", 1,
					"b", propertyReader(
							"a", 1,
							"b", listReader( "a", 1, true ) ) ) ) );

		final Map<Object, Object> objToObjMap = new LinkedHashMap<Object, Object>();
		objToObjMap.put( true, 1 );
		objToObjMap.put( false, 0 );
		objToObjMap.put( null, null );
		objToObjMap.put( 10, 1e1 );
		doTest( "{true:1,false:0,null:null,10:10.0}",
				write( objToObjMap ) );

		objToObjMap.put( propertyReader("a", 1), 1234 );
		doTest( "{true:1,false:0,null:null,10:10.0,{\\\"a\\\":1}:1234}",
				write( objToObjMap ) );


		// Maps of Dates
/*
		doTest( "{ \"nowJava\" : " + jsonNowJava + " }",
				write( propertyReader( "nowJava", nowJava ) ) );

		final Map<java.util.Date, String> dateToStringMap = new LinkedHashMap<java.util.Date, String>();
		dateToStringMap.put( nowJava, "nowJava" );
		doTest( "{ " + jsonNowJava + " : \"nowJava\" }",
				write( dateToStringMap ) );
*/

/*		// Bytes

		doTest( "\"bytes{}\"", write( new byte[0] ) );

		final String unencodedString = "unencoded string";
		final byte[] unencodedBytes = unencodedString.getBytes();
		final char[] encodedBytes = Base64Coder.encode(unencodedBytes); // "bytes{dW5lbmNvZGVkIHN0cmluZw==}"
		final String jsonString = String.format("\"bytes{%s}\"", new String(encodedBytes));
		doTest( jsonString, write( javaDateSerializer, unencodedBytes ) );
*/
	}

	//--------------------------------------------------

	public static void doTest(final Object expected, final Object actual) {

		final String message =
			String.format( "\texpected = %s\n\t  actual = %s\n", expected, actual );

		assertEquals( message, expected, actual );
	}

	//--------------------------------------------------
}
