package com.zz.sdk.entity.result;

import org.json.JSONObject;

public class ResultRequest extends BaseResult {

	private static final long serialVersionUID = -8453873063884063578L;

	protected static final String K_CMGE_ORDER_NUM = "cmgeOrderNum";

	public String mCmgeOrderNum;

	@Override
	public JSONObject buildJson() {
		try {
			JSONObject json = super.buildJson();
			setString(json, K_CMGE_ORDER_NUM, mCmgeOrderNum);
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void parseJson(JSONObject json) {
		if (json == null)
			return;
		try {
			super.parseJson(json);
			mCmgeOrderNum = getString(json, K_CMGE_ORDER_NUM);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
