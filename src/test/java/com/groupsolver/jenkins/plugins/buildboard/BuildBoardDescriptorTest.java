package com.groupsolver.jenkins.plugins.buildboard;

import static com.groupsolver.jenkins.plugins.buildboard.viewmodel.syntacticsugar.Loops.asFollows;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import hudson.util.FormValidation;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BuildBoardDescriptorTest {

    private JenkinsRule jenkins;

    private BuildBoardDescriptor validator;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        jenkins = rule;
        validator = new BuildBoardDescriptor();
    }

    @Test
    void form_validator_should_allow_valid_reg_ex_specifying_what_jobs_to_include() {
        for (String regex : asFollows(null, "", ".*", "myproject-.*")) {
            assertThat(itShouldAllow(regex), validator.doCheckIncludeRegex(regex).kind, is(FormValidation.Kind.OK));
        }
    }

    @Test
    void form_validator_should_advise_how_a_regex_could_be_improved() {
        FormValidation result = validator.doCheckIncludeRegex(")");

        assertThat(result.kind, is(FormValidation.Kind.ERROR));
        assertThat(htmlDecoded(result.getMessage()), containsString("Unmatched closing ')'"));
    }

    private String htmlDecoded(String message) {
        return StringEscapeUtils.unescapeHtml(message);
    }

    private String itShouldAllow(String regex) {
        return "should allow " + regex;
    }
}
