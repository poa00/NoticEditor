package com.temporaryteam.noticeditor.model;

import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class NoticeCategory {

	private String name;
	private ArrayList<NoticeCategory> subcategories;
	private String content;

	public NoticeCategory() {
		name = null;
		content = null;
		subcategories = null;
	}

	public NoticeCategory(String name, String content) {
		this.name = name;
		this.content = content;
	}

	public NoticeCategory(String name, ArrayList<NoticeCategory> subcategories) {
		this.name = name;
		this.subcategories = subcategories;
		content = "";
	}

	public String getName() {
		return name;
	}

	public ArrayList<NoticeCategory> getSubCategories() {
		return subcategories;
	}

	public String getContent() {
		return content;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSubCategories(ArrayList<NoticeCategory> subcategories) {
		this.subcategories = subcategories;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("content", content);
		ArrayList vect = new ArrayList<JSONObject>();
		if(subcategories!=null) for(NoticeCategory subcategory : subcategories) vect.add(subcategory.toJson());
		obj.put("subcategories", new JSONArray(vect));
		return obj;
	}

	public void fromJson(JSONObject jsobj) throws JSONException {
		name = jsobj.getString("name");
		JSONArray arr = jsobj.getJSONArray("subcategories");
		subcategories = new ArrayList<NoticeCategory>();
		NoticeCategory category = new NoticeCategory();
		if(arr.length()!=0) {
			for(int i = 0; i<arr.length(); i++) {
				category.fromJson(arr.getJSONObject(i));
				subcategories.add(category);
			}
		}
		else subcategories = null;
		content = jsobj.getString("content");
	}
}
