package org.iushu.market.component;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

public class JSONArrayAdapter extends JSONObject {

    private JSONArray jsonArray;

    private JSONArrayAdapter(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    @Override
    public String toJSONString(JSONWriter.Feature... features) {
        return this.jsonArray.toJSONString(features);
    }

    public static JSONArrayAdapter of(JSONArray jsonArray) {
        return new JSONArrayAdapter(jsonArray);
    }

    public static JSONArrayAdapter of(JSONObject jsonObject) {
        return new JSONArrayAdapter(JSONArray.of(jsonObject));
    }

}
