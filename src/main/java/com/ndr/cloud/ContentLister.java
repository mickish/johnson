package com.ndr.cloud;

import java.util.List;
import java.util.Map;

public interface ContentLister {

	List<Map<String,Object>> getContentLists(String customerCode);

	//List<Map<String,Object>> getContentListInfo(int id);

	//int upsertContentList(String customerCode, String contentListTitle, String contentListDescription, Integer contentListId);

	//void deleteContentList(String customerCode, int contentListId);

	//int upsertContentListItem(String customerCode, int bucketId, int itemId, String comment, Integer ordinal, Integer bucketItemsId);

	//void deleteContentListItem(String customerCode, int bucketItemId);

}
