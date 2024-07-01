// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package com.braintribe.devrock.ant.test.common;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import com.braintribe.transport.http.DefaultHttpClientProvider;

/**
 * helper functions for tests that are using the Repolet 
 * @author pit
 *
 */
public interface RepoletUsingTrait {
	
	/**
	 * @return - a configured {@link CloseableHttpClient}
	 */
	public static CloseableHttpClient client() {
		DefaultHttpClientProvider bean = new DefaultHttpClientProvider();
		bean.setSocketTimeout(60_000);
		try {
			CloseableHttpClient httpClient = bean.provideHttpClient();
			return httpClient;
		} catch (Exception e) {
			throw new IllegalStateException("",e);
		}
	}
	
	/**
	 * returns a {@link CloseableHttpResponse} from a {@link HttpHead} request
	 * @param httpClient - the {@link CloseableHttpClient}
	 * @param url - a {@link String} containing the URL
	 * @return - a {@link CloseableHttpResponse}
	 * @throws IOException
	 */
	public static CloseableHttpResponse retrieveHeadResponse( CloseableHttpClient httpClient, String url) throws IOException {
		HttpRequestBase requestBase = new HttpHead( url);
		HttpClientContext context = HttpClientContext.create();
		CloseableHttpResponse response = httpClient.execute( requestBase, context);
		return response;
	}
	/**
	 * returns a {@link CloseableHttpResponse} from a {@link HttpOptions} request
	 * @param httpClient - the {@link CloseableHttpClient}
	 * @param url - a {@link String} containing the URL
	 * @return - a {@link CloseableHttpResponse}
	 * @throws IOException
	 */
	
	public static  CloseableHttpResponse retrieveOptionsResponse( CloseableHttpClient httpClient, String url) throws IOException {
		HttpRequestBase requestBase = new HttpOptions( url);
		HttpClientContext context = HttpClientContext.create();
		CloseableHttpResponse response = httpClient.execute( requestBase, context);
		return response;
	}
	/**
	 * returns a {@link CloseableHttpResponse} from a {@link HttpGet} request
	 * @param httpClient - the {@link CloseableHttpClient}
	 * @param url - a {@link String} containing the URL
	 * @return - a {@link CloseableHttpResponse}
	 * @throws IOException
	 */

	public static  CloseableHttpResponse retrieveGetResponse( CloseableHttpClient httpClient, String url) throws IOException {
		HttpRequestBase requestBase = new HttpGet( url);
		HttpClientContext context = HttpClientContext.create();
		CloseableHttpResponse response = httpClient.execute( requestBase, context);
		return response;
	}
}
