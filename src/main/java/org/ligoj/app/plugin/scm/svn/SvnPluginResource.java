package org.ligoj.app.plugin.scm.svn;

import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.scm.AbstractIndexBasedPluginResource;
import org.ligoj.app.plugin.scm.ScmResource;
import org.ligoj.app.plugin.scm.ScmServicePlugin;
import org.springframework.stereotype.Component;

/**
 * Subversion resource.
 */
@Path(SvnPluginResource.URL)
@Component
@Produces(MediaType.APPLICATION_JSON)
public class SvnPluginResource extends AbstractIndexBasedPluginResource implements ScmServicePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String URL = ScmResource.SERVICE_URL + "/svn";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Constructor specifying a Subversion implementation.
	 */
	public SvnPluginResource() {
		super(KEY, "svn");
	}

	@Override
	protected String getRepositoryUrl(final Map<String, String> parameters) {
		// For SVN, a trailing "/" is added.
		return StringUtils.appendIfMissing(super.getRepositoryUrl(parameters), "/");
	}

	/**
	 * Return the revision number.
	 */
	@Override
	protected Object toData(final String statusContent) {
		final int rindex = statusContent.indexOf("Revision ");
		final int lindex = statusContent.indexOf(':', rindex + 1);
		return Integer.parseInt(StringUtils.trim(statusContent.substring(rindex + "Revision ".length(), lindex)));
	}
}
