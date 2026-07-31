package com.neubofy.veto.net

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import org.json.JSONException
import org.json.JSONObject


/**
 * Same as a [JsonObjectRequest], but expecting an empty response.
 *
 * This is useful when POST-ing data somewhere, where a successful response
 * has an empty body.
 * [JsonObjectRequest] tries to deserialise the response body into a JSON object
 * which then causes a [JSONException].
 */
class JsonPostRequest(
    method: Int,
    url: String,
    jsonRequest: JSONObject,
    listener: Listener<JSONObject>,
    errorListener: ErrorListener,
) : JsonRequest<JSONObject>(
    method, url, jsonRequest.toString(), listener, errorListener
) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject>? {
        // The parsed result must have the same type as the request.
        // Thus we cannot return a Unit, we must return a JSONObject.
        val parsed = JSONObject()
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun getHeaders(): Map<String, String> {
        return mapOf<String, String>(
            Pair(HEADER_USER_AGENT, Veto_USER_AGENT)
        )
    }
}
