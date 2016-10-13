package org.icgc.dcc.storage.report.core;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.val;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.util.Base64Utils;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * From Reference TestRestTemplate class in Spring Boot:
 * https://github.com/spring-projects/spring-boot/blob/master/spring-boot/src/main/java/org/springframework/boot/test/TestRestTemplate.java
 *
 */
public class BasicAuthRestTemplate extends RestTemplate {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	/**
	 * Create a new {@link TestRestTemplate} instance.
	 * 
	 * @param httpClientOptions	client options to use if the Apache HTTP Client is used
	 */
	public BasicAuthRestTemplate(HttpClientOption... httpClientOptions) {
		this(null, null, httpClientOptions);
	}

	/**
	 * Create a new {@link TestRestTemplate} instance with the specified credentials.
	 * 
	 * @param username	the username to use (or {@code null})
	 * @param password	the password (or {@code null})
	 * @param httpClientOptions	client options to use if the Apache HTTP Client is used
	 */
	public BasicAuthRestTemplate(String username, String password, HttpClientOption... httpClientOptions) {
		if (ClassUtils.isPresent("org.apache.http.client.config.RequestConfig", null)) {
			setRequestFactory(new CustomHttpComponentsClientHttpRequestFactory(httpClientOptions));
		}
		addAuthentication(username, password);
		setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});

	}

	private void addAuthentication(String username, String password) {
		if (username == null) {
			return;
		}
		List<ClientHttpRequestInterceptor> interceptors = Collections
				.<ClientHttpRequestInterceptor> singletonList(new BasicAuthorizationInterceptor(username, password));
		setRequestFactory(new InterceptingClientHttpRequestFactory(getRequestFactory(), interceptors));
	}

	/**
	 * Options used to customize the Apache Http Client if it is used.
	 */
	public enum HttpClientOption {

		/**
		 * Enable cookies.
		 */
		ENABLE_COOKIES,

		/**
		 * Enable redirects.
		 */
		ENABLE_REDIRECTS

	}

	private static class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

		private final String username;
		private final String password;

		BasicAuthorizationInterceptor(String username, String password) {
			this.username = username;
			this.password = (password == null ? "" : password);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			val token = Base64Utils.encodeToString((this.username + ":" + this.password).getBytes(UTF_8));
			request.getHeaders().add("Authorization", "Basic " + token);
			return execution.execute(request, body);
		}
	}

	/**
	 * {@link HttpComponentsClientHttpRequestFactory} to apply customizations.
	 */
	protected static class CustomHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

		private final String cookieSpec;

		private final boolean enableRedirects;

		public CustomHttpComponentsClientHttpRequestFactory(HttpClientOption[] httpClientOptions) {
			Set<HttpClientOption> options = new HashSet<BasicAuthRestTemplate.HttpClientOption>(Arrays.asList(httpClientOptions));
			this.cookieSpec = (options.contains(HttpClientOption.ENABLE_COOKIES) ? CookieSpecs.STANDARD
					: CookieSpecs.IGNORE_COOKIES);
			this.enableRedirects = options.contains(HttpClientOption.ENABLE_REDIRECTS);
		}

		@Override
		protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
			val context = HttpClientContext.create();
			context.setRequestConfig(getRequestConfig());
			return context;
		}

		protected RequestConfig getRequestConfig() {
			val builder = RequestConfig.custom().setCookieSpec(this.cookieSpec).setAuthenticationEnabled(false)
					.setRedirectsEnabled(this.enableRedirects);
			return builder.build();
		}

	}
}
