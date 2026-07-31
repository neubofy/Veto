package com.neubofy.veto.net

import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener


/**
 * Same as a [com.android.volley.toolbox.StringRequest], but sets the User-Agent.
 */
class StringRequest(
    method: Int,
    url: String,
    listener: Listener<String>,
    errorListener: ErrorListener,
) : com.android.volley.toolbox.StringRequest(
    method, url, listener, errorListener
) {
    override fun getHeaders(): Map<String, String> {
        return mapOf<String, String>(
            Pair(HEADER_USER_AGENT, Veto_USER_AGENT)
        )
    }
}
