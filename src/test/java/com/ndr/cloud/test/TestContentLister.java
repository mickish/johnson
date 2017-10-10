package com.ndr.cloud.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ndr.cloud.ContentLister;
import com.ndr.cloud.ContentListerJersey;


public class TestContentLister {

	@Test
	public void testGetContentLists() {
		final ContentLister contentLister = new ContentListerJersey();
		final List<Map<String,Object>> contentLists = contentLister.getContentLists("MMT394");

		System.out.println( contentLists );
	}

}
