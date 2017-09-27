package johnson.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestJohnson {

	@Test
	public void test() {

		Map<String,Object> map = new HashMap<>();
		map.put( "key1","val1" );
		map.put( "key2","val2" );
		map.put( "key3","val3" );

		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonResult = mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(map);

			System.out.println( jsonResult );

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
