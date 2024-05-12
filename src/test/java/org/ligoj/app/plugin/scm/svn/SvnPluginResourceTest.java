package org.ligoj.app.plugin.scm.svn;

import jakarta.transaction.Transactional;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractServerTest;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test class of {@link SvnPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class SvnPluginResourceTest extends AbstractServerTest {
	@Autowired
	private SvnPluginResource resource;

	@Autowired
	private SubscriptionResource subscriptionResource;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	protected int subscription;

	@BeforeEach
	void prepareData() throws IOException {
		// Only with Spring context
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8);
		this.subscription = getSubscription("Jupiter");

		// Coverage only
		Assertions.assertEquals("service:scm:svn",resource.getKey());
	}

	/**
	 * Return the subscription identifier of the given project. Assumes there is
	 * only one subscription for a service.
	 */
	private Integer getSubscription(final String project) {
		return getSubscription(project, SvnPluginResource.KEY);
	}

	@Test
	void delete() throws Exception {
		resource.delete(subscription, false);
		em.flush();
		em.clear();
		// No custom data -> nothing to check;
	}

	@Test
	void getVersion() throws Exception {
		Assertions.assertNull(resource.getVersion(subscription));
	}

	@Test
	void getLastVersion() throws Exception {
		Assertions.assertNull(resource.getLastVersion());
	}

	@Test
	void link() throws Exception {
		prepareMockRepository();
		httpServer.start();

		// Invoke create for an already created entity, since for now, there is
		// nothing but validation pour SonarQube
		resource.link(this.subscription);

		// Nothing to validate for now...
	}

	@Test
	void linkNotFound() throws Exception {
		prepareMockRepository();
		httpServer.start();

		parameterValueRepository.findAllBySubscription(subscription).stream()
				.filter(v -> v.getParameter().getId().equals(SvnPluginResource.KEY + ":repository")).findFirst().get().setData("0");
		em.flush();
		em.clear();

		// Invoke create for an already created entity, since for now, there is
		// nothing but validation pour SonarQube
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.link(this.subscription)), "service:scm:svn:repository", "svn-repository");
	}

	@Test
	void checkSubscriptionStatus() throws Exception {
		prepareMockRepository();
		final var nodeStatusWithData = resource.checkSubscriptionStatus(subscriptionResource.getParametersNoCheck(subscription));
		Assertions.assertTrue(nodeStatusWithData.getStatus().isUp());
		Assertions.assertEquals(2039, nodeStatusWithData.getData().get("info"));
	}

	private void prepareMockRepository() throws IOException {
		httpServer.stubFor(get(urlPathEqualTo("/ligoj-jupiter/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/scm/svn/svn-repo.html").getInputStream(), StandardCharsets.UTF_8))));
		httpServer.start();
	}

	private void prepareMockAdmin() throws IOException {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)
				.withBody(IOUtils.toString(new ClassPathResource("mock-server/scm/index.html").getInputStream(), StandardCharsets.UTF_8))));
		httpServer.start();
	}

	@Test
	void checkStatus() throws Exception {
		prepareMockAdmin();
		final var parameters = subscriptionResource.getParametersNoCheck(this.subscription);
		Assertions.assertTrue(resource.checkStatus(parameters));
	}

	@Test
	void checkStatusAuthenticationFailed() {
		startAndCheckFail();
	}

	@Test
	void checkStatusNotAdmin() {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		startAndCheckFail();
	}

	@Test
	void checkStatusInvalidIndex() {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("<html>some</html>")));
		startAndCheckFail();
	}

	private void startAndCheckFail() {
		httpServer.start();
		final var parameters = subscriptionResource.getParametersNoCheck(this.subscription);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkStatus(parameters)), SvnPluginResource.KEY + ":url", "svn-admin");
	}

	@Test
	void findAllByName() throws IOException {
		prepareMockAdmin();
		httpServer.start();

		final var projects = resource.findAllByName("service:scm:svn:dig", "as-");
		Assertions.assertEquals(4, projects.size());
		Assertions.assertEquals("has-event", projects.getFirst().getId());
		Assertions.assertEquals("has-event", projects.getFirst().getName());
	}

	@Test
	void findAllByNameNoListing() {
		httpServer.start();

		final var projects = resource.findAllByName("service:scm:svn:dig", "as-");
		Assertions.assertEquals(0, projects.size());
	}

}
