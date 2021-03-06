package com.github.searls.jasmine.server;

import static org.hamcrest.Matchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.searls.jasmine.AbstractJasmineMojo;
import com.github.searls.jasmine.CreatesRunner;
import com.github.searls.jasmine.NullLog;
import com.github.searls.jasmine.coffee.DetectsCoffee;
import com.github.searls.jasmine.coffee.HandlesRequestsForCoffee;
import com.github.searls.jasmine.runner.ReporterType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JasmineResourceHandler.class)
public class JasmineResourceHandlerTest {
	private static final String TARGET = "some url";
	private static final String RUNNER_FILE_NAME = "runnerfile.html";

	@Mock private DetectsCoffee detectsCoffee;
	@Mock private HandlesRequestsForCoffee handlesRequestsForCoffee;
	@Mock private CreatesRunner createsManualRunner;

	@Mock AbstractJasmineMojo config;
	@Mock Request baseRequest;
	@Mock HttpServletRequest request;
	@Mock HttpServletResponse response;
	@Mock Resource resource;

	@Mock Log log;

	@InjectMocks private final JasmineResourceHandler subject = new JasmineResourceHandler(mock(AbstractJasmineMojo.class),"",ReporterType.HtmlReporter) {
		@Override
		protected Resource getResource(HttpServletRequest request) throws MalformedURLException {
			return JasmineResourceHandlerTest.this.resource;
		}
	};

	@Test
	public void constructorSetsLoggingLow() throws Exception {
		whenNew(CreatesRunner.class).withArguments(this.config,RUNNER_FILE_NAME,ReporterType.HtmlReporter).thenReturn(this.createsManualRunner);

		new JasmineResourceHandler(this.config,RUNNER_FILE_NAME,ReporterType.HtmlReporter);

		verify(this.createsManualRunner).setLog(argThat(isA(NullLog.class)));
	}

	@Test
	public void whenTargetIsSlashThenCreateManualRunner() throws IOException, ServletException {
		this.subject.handle("/", this.baseRequest,this.request,this.response);

		verify(this.createsManualRunner).create();
	}

	@Test
	public void whenTargetIsNotSlashThenCreateManualRunner() throws IOException, ServletException {
		this.subject.handle("/notSlash", this.baseRequest,this.request,this.response);

		verify(this.createsManualRunner,never()).create();
	}

	@Test
	public void whenCoffeeDelegatesToCoffeeHandler() throws IOException, ServletException {
		when(this.detectsCoffee.detect(TARGET)).thenReturn(true);
		when(this.resource.exists()).thenReturn(true);

		this.subject.handle(TARGET, this.baseRequest,this.request,this.response);

		verify(this.handlesRequestsForCoffee).handle(this.baseRequest, this.response, this.resource);
	}

	@Test
	public void whenNotCoffeeDoesNotDelegateToCoffeeHandler() throws IOException, ServletException {
		when(this.detectsCoffee.detect(TARGET)).thenReturn(false);
		when(this.resource.exists()).thenReturn(true);

		this.subject.handle(TARGET, this.baseRequest,this.request,this.response);

		verify(this.handlesRequestsForCoffee, never()).handle(any(Request.class), any(HttpServletResponse.class), any(Resource.class));
	}

	@Test
	public void whenCoffeeButResourceIsHandledDoNotDelegateToCoffeeHandler() throws IOException, ServletException {
		when(this.detectsCoffee.detect(TARGET)).thenReturn(true);
		when(this.resource.exists()).thenReturn(true);
		when(this.baseRequest.isHandled()).thenReturn(true);

		this.subject.handle(TARGET, this.baseRequest,this.request,this.response);

		verify(this.handlesRequestsForCoffee, never()).handle(any(Request.class), any(HttpServletResponse.class), any(Resource.class));
	}

	@Test
	public void whenCoffeeButDoesNotExistDoNotDelegateToCoffeeHandler() throws IOException, ServletException {
		when(this.detectsCoffee.detect(TARGET)).thenReturn(true);
		when(this.resource.exists()).thenReturn(false);

		this.subject.handle(TARGET, this.baseRequest,this.request,this.response);

		verify(this.handlesRequestsForCoffee, never()).handle(any(Request.class), any(HttpServletResponse.class), any(Resource.class));
	}

	@Test
	public void whenCoffeeButResourceIsNullDoNotDelegateToCoffeeHandler() throws IOException, ServletException {
		when(this.detectsCoffee.detect(TARGET)).thenReturn(true);
		this.resource = null;

		this.subject.handle(TARGET, this.baseRequest,this.request,this.response);

		verify(this.handlesRequestsForCoffee, never()).handle(any(Request.class), any(HttpServletResponse.class), any(Resource.class));
	}

}
