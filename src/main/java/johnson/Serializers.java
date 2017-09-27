package johnson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class Serializers {

	/** Do not create instances of this class */
	private Serializers() {}

	//--------------------------------------------------
	//  Date to JSON String conversion routines
	//--------------------------------------------------

	/** "date{yyyy-MM-dd-HH-mm-ss-SSS}" */
	protected static final ThreadLocal<DateFormat> tlDateLongFormatJsonRpc =
		new ThreadLocal<DateFormat>() {
			@Override
			protected synchronized DateFormat initialValue() {
				final DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
				df.setLenient( false );
				return df;
			}
	};

	/** "date{yyyy-MM-dd}" */
	protected static final ThreadLocal<DateFormat> tlDateShortFormatJsonRpc =
		new ThreadLocal<DateFormat>() {
			@Override
			protected synchronized DateFormat initialValue() {
				final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				df.setLenient( false );
				return df;
			}
	};

	protected static final ThreadLocal<ObjectMapper> tlObjectMapper =
		new ThreadLocal<ObjectMapper>() {

			@Override
			protected synchronized ObjectMapper initialValue() {

				final ObjectMapper mapper = new ObjectMapper();

				mapper.getSerializerProvider().setNullKeySerializer(new CustomNullKeySerializer(null));

				final SimpleModule simpleModule = new SimpleModule();
				simpleModule.addKeySerializer(Boolean.class, new CustomPrimitiveKeySerializer<Boolean>(Boolean.class));
				simpleModule.addKeySerializer(Integer.class, new CustomPrimitiveKeySerializer<Integer>(Integer.class));
				simpleModule.addKeySerializer(Number.class, new CustomPrimitiveKeySerializer<Number>(Number.class));

				simpleModule.addKeySerializer(Map.class, new CustomObjectKeySerializer<Map>( mapper ));

				simpleModule.addSerializer(Date.class, new CustomDateSerializer(Date.class));
				simpleModule.addDeserializer(Object.class, new CustomDateDeserializer());
				mapper.registerModule( simpleModule );

				return mapper;
			}
	};

	public static String write (final Object obj) {
		try {
			return tlObjectMapper.get().writeValueAsString( obj );
		} catch (final JsonProcessingException e) {
			throw new RuntimeException( "While writing " + obj, e );
		}
	}

	/**
	 * This is different than com.fasterxml.jackson.databind.ser.std.NullSerializer
	 * because it uses gen.writeFieldName() to adjust the writer's state, indicating
	 * that it is writing a key, and that the next thing to be written will be a value.
	 * It also avoids wrapping the key name in quotes, so that the key is really null.
	 */
	public static class CustomNullKeySerializer extends StdSerializer<Object> {

		private static final long serialVersionUID = 1L;

		public CustomNullKeySerializer (final Class<Object> valueClass) {
			super( valueClass );
		}

		@Override
		public void serialize(final Object nullKey, final JsonGenerator gen, final SerializerProvider unused)
				throws IOException {

			Arrays.stream( JsonGenerator.Feature.values() )
				.forEach( f -> {
					System.out.printf( "%s %s\n", f, gen.isEnabled(f) );
				} );

			try {
				gen.disable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
				gen.writeFieldName("null");
			} finally {
				gen.enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
			}
		}
	}

	/** Don't quote primitive keys like true, false, null, and numbers */
	public static class CustomPrimitiveKeySerializer<T> extends StdSerializer<T> {

		private static final long serialVersionUID = 1L;

		public CustomPrimitiveKeySerializer (final Class<T> valueClass) {
			super( valueClass );
		}

		@Override
		public void serialize(final T key, final JsonGenerator gen, final SerializerProvider unused)
				throws IOException {

			try {
				gen.disable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
				gen.writeFieldName( key.toString() );
			} finally {
				gen.enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
			}
		}
	}

	/** Support maps with non-string keys */
	public static class CustomObjectKeySerializer<T> extends JsonSerializer<T>
	{
		final ObjectMapper mapper;

	    public CustomObjectKeySerializer (final ObjectMapper mapper) {
	    	this.mapper = mapper;
	    }

	    @Override
	    public void serialize (final T value, final JsonGenerator gen, final SerializerProvider provider)
	    		throws IOException
	    {
			try {
				gen.disable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
				gen.writeFieldName( mapper.writeValueAsString( value ) );
			} finally {
				gen.enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
			}
	    }
	}

	public static class CustomDateSerializer extends StdSerializer<Date> {

		private static final long serialVersionUID = 1L;

		public CustomDateSerializer(final Class<Date> valueClass) {
			super( valueClass );
		}

		@Override
		public void serialize(final Date date, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
			gen.writeRaw( "\"" );
			gen.writeRaw( formatDateJsonRpc(date) );
			gen.writeRaw( "\"" );
		}

	}

	public static class CustomDateDeserializer extends StdDeserializer<Date> {

		private static final long serialVersionUID = 1L;

		public CustomDateDeserializer() {
			this(null);
		}

		public CustomDateDeserializer(final Class<Date> valueClass) {
			super(valueClass);
		}

		private static final String dateLongSubPattern =
			"\\d\\d\\d\\d-[01]?\\d-[0123]?\\d-\\d{1,2}-\\d{1,2}-\\d{1,2}-\\d{1,3}";
		private static final String dateShortSubPattern =
			"\\d\\d\\d\\d-[01]?\\d-[0123]?\\d";

		private static final Pattern dateLongPatternJsonRpc =
			Pattern.compile( "date\\{("+dateLongSubPattern+")\\}" );
		private static final Pattern dateShortPatternJsonRpc =
			Pattern.compile( "date\\{("+dateShortSubPattern+")\\}" );
		private static final Pattern dateIso8601Pattern =
			Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2}).(\\d{3})Z");

		//--------------------------------------------------

		/**
		 * Given a string containing a date in JsonRpc format "date{yyyy-MM-dd-HH-mm-ss-SSS}"
		 * or "date{yyyy-MM-dd}, return a java.util.Date
		 */
		public static java.util.Date parseDateJsonRpc (final String s) {

			try {
				Matcher m = dateLongPatternJsonRpc.matcher( s );
				if( m.matches() ) {
					return tlDateLongFormatJsonRpc.get().parse( m.group(1) );
				}

				m = dateShortPatternJsonRpc.matcher( s );
				if( m.matches() ) {
					return tlDateShortFormatJsonRpc.get().parse( m.group(1) );
				}

				m = dateIso8601Pattern.matcher( s );
				if ( m.matches() ) {
					final String longFormattedDate = m.group(1) + "-" + m.group(2).replace(':', '-') + "-" + m.group(3);
					return tlDateLongFormatJsonRpc.get().parse( longFormattedDate );
				}
			} catch (final ParseException e) {
				throw new RuntimeException( "While parsing "+s );
			}

			throw new RuntimeException( s + " is not a JSON-RPC date" );
		}

		@Override
		public Date deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return parseDateJsonRpc( p.getText() );
		}
	}

	public static Date parseDateJsonRpc (final String s) {
		try {
			final TypeReference<Object> type = new TypeReference<Object>() {};
			final Object obj = tlObjectMapper.get().readValue(s, type);
			return (Date)obj;
		} catch ( final IOException e) {
			throw new RuntimeException( "While parsing "+s, e );
		}
	}

	/**
	 * Short date strings like "2008-01-01"
	 */
	public static String formatDate(final java.util.Date jDate) {
		return tlDateShortFormatJsonRpc.get().format( jDate );
	}

	/**
	 * Custom date strings like "yyyy-MM-dd hh:mm:ss a"
	 */
	public static String formatDate(final String formatString, final java.util.Date jDate) {
		final SimpleDateFormat formatter = new java.text.SimpleDateFormat( formatString );
		return formatter.format( jDate );
	}

	//--------------------------------------------------

	/**
	 * Returns a string representation of the date in JsonRpc format
	 * "date{yyyy-MM-dd-HH-mm-ss-SSS}", where MM is the 1-based month,
	 * dd is the 1-based day of the month, and YYYY is the four-digit year.
	 */
	public static String formatDateJsonRpc (final java.util.Date date) {
		return "date{" + tlDateLongFormatJsonRpc.get().format( date ) + "}";
	}

	//--------------------------------------------------

	/** java.util.Date constructor for short ISO-8601 style strings (e.g., "2008-01-01") */
	public static java.util.Date jDate(final String s) {
		try {
			return tlDateShortFormatJsonRpc.get().parse( s );
		} catch (final ParseException e) {
			throw new RuntimeException( "While parsing "+s, e );
		}
	}

	//--------------------------------------------------
}
