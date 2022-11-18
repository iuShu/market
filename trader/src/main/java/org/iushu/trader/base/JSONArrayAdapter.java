package org.iushu.trader.base;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

public class JSONArrayAdapter extends JSONObject {

    private JSONArray jsonArray;

    public JSONArrayAdapter(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    @Override
    public String toJSONString(JSONWriter.Feature... features) {
        return this.jsonArray.toJSONString(features);
    }
}
