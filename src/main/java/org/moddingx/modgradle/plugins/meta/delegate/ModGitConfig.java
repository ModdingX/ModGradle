package org.moddingx.modgradle.plugins.meta.delegate;

import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

public class ModGitConfig {

    @Nullable public URI url;
    @Nullable public URI clone;
    @Nullable public URI issues;
    @Nullable public String commitFormat;

    public Delegate delegate() {
        return new Delegate();
    }

    public class Delegate {

        public void url(String url) throws URISyntaxException {
            this.url(new URI(url));
        }

        public void url(URI url) {
            ModGitConfig.this.url = url;
        }

        public void clone(String clone) throws URISyntaxException {
            this.clone(new URI(clone));
        }

        public void clone(URI clone) {
            ModGitConfig.this.clone = clone;
        }

        public void issues(String issues) throws URISyntaxException {
            this.clone(new URI(issues));
        }

        public void issues(URI issues) {
            ModGitConfig.this.issues = issues;
        }

        public void commitFormat(String commitFormat) {
            ModGitConfig.this.commitFormat = commitFormat;
        }

        public void github(String repo) throws URISyntaxException {
            this.url("https://github.com/" + repo);
            this.clone("https://github.com/" + repo + ".git");
            this.issues("https://github.com/" + repo + "/issues");
            this.commitFormat("https://github.com/" + repo + "/commit/%H");
        }

        public void gitlab(String repo) throws URISyntaxException {
            this.url("https://gitlab.com/" + repo);
            this.clone("https://gitlab.com/" + repo + ".git");
            this.issues("https://gitlab.com/" + repo + "/-/issues");
            this.commitFormat("https://gitlab.com/" + repo + "/-/commit/%H");
        }
    }
}
