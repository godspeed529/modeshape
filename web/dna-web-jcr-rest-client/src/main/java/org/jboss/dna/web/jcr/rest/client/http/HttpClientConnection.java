/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.http;

import static org.jboss.dna.web.jcr.rest.client.RestClientI18n.unknownHttpRequestMethodMsg;
import java.net.URL;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * <code>HttpClientConnection</code> uses the <code>Apache HttpClient</code>.
 */
public final class HttpClientConnection {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The HTTP client that opens the connection, executes the request, and closes the connection.
     */
    private final AbstractHttpClient httpClient;

    /**
     * The HTTP request.
     */
    private final HttpRequestBase request;

    /**
     * The HTTP response.
     */
    private HttpResponse response;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server with the host and port information (never <code>null</code>)
     * @param url the URL that will be used in the request (never <code>null</code>)
     * @param method the HTTP request method (never <code>null</code>)
     * @throws Exception if there is a problem establishing the connection
     */
    public HttpClientConnection( Server server,
                                 URL url,
                                 RequestMethod method ) throws Exception {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$
        CheckArg.isNotNull(url, "url"); //$NON-NLS-1$
        CheckArg.isNotNull(method, "method"); //$NON-NLS-1$

        this.httpClient = new DefaultHttpClient();
        this.httpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort()),
                                                                new UsernamePasswordCredentials(server.getUser(),
                                                                                                server.getPassword()));
        // determine the request type
        if (RequestMethod.GET == method) {
            this.request = new HttpGet();
        } else if (RequestMethod.DELETE == method) {
            this.request = new HttpDelete();
        } else if (RequestMethod.POST == method) {
            this.request = new HttpPost();
        } else if (RequestMethod.PUT == method) {
            this.request = new HttpPut();
        } else {
            throw new RuntimeException(unknownHttpRequestMethodMsg.text(method));
        }

        // set request URI
        this.request.setURI(url.toURI());
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * Disconnects this connection.
     */
    public void disconnect() {
        this.httpClient.getConnectionManager().shutdown();
    }

    /**
     * @return the HTTP response
     * @throws Exception if there is a problem obtaining the response
     */
    private HttpResponse getResponse() throws Exception {
        if (this.response == null) {
            this.response = this.httpClient.execute(request);
        }

        return this.response;
    }

    /**
     * @return the HTTP response code
     * @throws Exception if there is a problem getting the response code
     */
    public int getResponseCode() throws Exception {
        return getResponse().getStatusLine().getStatusCode();
    }

    /**
     * @return the HTTP response body as a string
     * @throws Exception if there is a problem reading from the connection
     */
    public String read() throws Exception {
        return EntityUtils.toString(getResponse().getEntity());
    }

    /**
     * @param bytes the bytes being posted to the HTTP connection (never <code>null</code>)
     * @throws Exception if there is a problem writing to the connection
     */
    public void write( byte[] bytes ) throws Exception {
        CheckArg.isNotNull(bytes, "bytes"); //$NON-NLS-1$

        ByteArrayEntity entity = new ByteArrayEntity(bytes);
        entity.setContentType(MediaType.APPLICATION_JSON);

        if (this.request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)this.request).setEntity(entity);
        }
    }

}
