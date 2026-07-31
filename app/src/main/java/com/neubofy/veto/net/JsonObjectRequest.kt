package com.neubofy.veto.net

import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import org.json.JSONObject


/**
 * Same as a [com.android.volley.toolbox.JsonObjectRequest], but sets the User-Agent.
 */
class JsonObjectRequest(
    method: Int,
    url: String,
    jsonRequest: JSONObject?,
    listener: Listener<JSONObject>,
    errorListener: ErrorListener,
) : com.android.volley.toolbox.JsonObjectRequest(
    method, url, jsonRequest, listener, errorListener
) {
    override fun getHeaders(): Map<String, String> {
        return mapOf<String, String>(
            Pair(HEADER_USER_AGENT, Veto_USER_AGENT)
        )
    }
}
