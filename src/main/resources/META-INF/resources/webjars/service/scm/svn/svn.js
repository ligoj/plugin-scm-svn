define(function () {
	var current = {

		configureSubscriptionParameters: function (configuration) {
			current.$super('registerXServiceSelect2')(configuration, 'service:scm:svn:repository', 'service/scm/svn/', null, true, null, false);
		},

		/**
		 * Render Subversion repository.
		 */
		renderKey: function (subscription) {
			return current.$super('renderKey')(subscription, 'service:scm:svn:repository');
		},

		/**
		 * Render Subversion home page.
		 */
		renderFeatures: function (subscription) {
			return current.$super('renderFeaturesScm')(subscription, 'svn');
		},

		/**
		 * Render SVN details : id, and amount of revisions.
		 */
		renderDetailsKey: function (subscription) {
			return current.$super('generateCarousel')(subscription, [current.renderKey(subscription), '#Revisions : ' + subscription.data.info]);
		}
	};
	return current;
});
