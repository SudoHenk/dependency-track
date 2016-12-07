/*
 * This file is part of Dependency-Track.
 *
 * Dependency-Track is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-Track is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Dependency-Track. If not, see http://www.gnu.org/licenses/.
 */
package org.owasp.dependencytrack.parser.dependencycheck;

import org.junit.Assert;
import org.junit.Test;
import org.owasp.dependencytrack.parser.dependencycheck.model.Analysis;
import org.owasp.dependencytrack.parser.dependencycheck.model.Dependency;

import java.io.File;

public class DependencyCheckParserTest {

    @Test
    public void parseTest() throws Exception {
        File file = new File("src/test/resources/dependency-check-report.xml");
        Analysis analysis = new DependencyCheckParser().parse(file);

        Assert.assertEquals("1.4.4", analysis.getScanInfo().getEngineVersion());
        Assert.assertEquals(17, analysis.getScanInfo().getDataSources().size());
        Assert.assertEquals("My Example Application", analysis.getProjectInfo().getName());
        Assert.assertEquals("2016-12-06T20:08:02.748-0700", analysis.getProjectInfo().getReportDate());
        Assert.assertEquals("This report contains data retrieved from the National Vulnerability Database: http://nvd.nist.gov", analysis.getProjectInfo().getCredits());
        Assert.assertEquals(1034, analysis.getDependencies().size());

        int foundCount = 0;
        for (Dependency dependency: analysis.getDependencies()) {
            if (dependency.getFileName().equals("bootstrap.jar")) {
                foundCount++;
                Assert.assertEquals("/workspace/apache-tomcat-7.0.52/bin/bootstrap.jar", dependency.getFilePath());
                Assert.assertEquals("ed56f95fdb8ee2903d821253f09f49f4", dependency.getMd5());
                Assert.assertEquals("d841369b0cf38390d451015786b6a8ff803d22cd", dependency.getSha1());

                Assert.assertEquals(12, dependency.getEvidenceCollected().size());
                Assert.assertEquals("vendor", dependency.getEvidenceCollected().get(0).getType());
                Assert.assertEquals("HIGH", dependency.getEvidenceCollected().get(0).getConfidence());
                Assert.assertEquals("file", dependency.getEvidenceCollected().get(0).getSource());
                Assert.assertEquals("name", dependency.getEvidenceCollected().get(0).getName());
                Assert.assertEquals("bootstrap", dependency.getEvidenceCollected().get(0).getValue());

                Assert.assertEquals(2, dependency.getIdentifiers().getIdentifiers().size());
                Assert.assertEquals("cpe", dependency.getIdentifiers().getIdentifiers().get(0).getType());
                Assert.assertEquals("HIGH", dependency.getIdentifiers().getIdentifiers().get(0).getConfidence());
                Assert.assertEquals("(cpe:/a:apache:tomcat:7.0.52)", dependency.getIdentifiers().getIdentifiers().get(0).getName());
                Assert.assertEquals("https://web.nvd.nist.gov/view/vuln/search-results?adv_search=true&cves=on&cpe_version=cpe%3A%2Fa%3Aapache%3Atomcat%3A7.0.52", dependency.getIdentifiers().getIdentifiers().get(0).getUrl());

                Assert.assertEquals(18, dependency.getVulnerabilities().getVulnerabilities().size());
                Assert.assertEquals("CVE-2016-6325", dependency.getVulnerabilities().getVulnerabilities().get(0).getName());

                Assert.assertEquals("7.2", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssScore());
                Assert.assertEquals("LOCAL", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssAccessVector());
                Assert.assertEquals("LOW", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssAccessComplexity());
                Assert.assertEquals("NONE", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssAuthenticationr());
                Assert.assertEquals("COMPLETE", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssConfidentialImpact());
                Assert.assertEquals("COMPLETE", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssIntegrityImpact());
                Assert.assertEquals("COMPLETE", dependency.getVulnerabilities().getVulnerabilities().get(0).getCvssAvailabilityImpact());
                Assert.assertEquals("High", dependency.getVulnerabilities().getVulnerabilities().get(0).getSeverity());
                Assert.assertEquals("CWE-264 Permissions, Privileges, and Access Controls", dependency.getVulnerabilities().getVulnerabilities().get(0).getCwe());
                Assert.assertEquals("The Tomcat package on Red Hat Enterprise Linux (RHEL) 5 through 7, JBoss Web Server 3.0, and JBoss EWS 2 uses weak permissions for (1) /etc/sysconfig/tomcat and (2) /etc/tomcat/tomcat.conf, which allows local users to gain privileges by leveraging membership in the tomcat group.", dependency.getVulnerabilities().getVulnerabilities().get(0).getDescription());
                Assert.assertEquals(4, dependency.getVulnerabilities().getVulnerabilities().get(0).getReferences().size());
                Assert.assertEquals("REDHAT", dependency.getVulnerabilities().getVulnerabilities().get(0).getReferences().get(3).getSource());
                Assert.assertEquals("RHSA-2016:2046", dependency.getVulnerabilities().getVulnerabilities().get(0).getReferences().get(3).getName());
                Assert.assertEquals("http://www.securityfocus.com/bid/91453", dependency.getVulnerabilities().getVulnerabilities().get(3).getReferences().get(0).getUrl());
                Assert.assertEquals(1, dependency.getVulnerabilities().getVulnerabilities().get(0).getVulnerableSoftware().size());
                Assert.assertEquals("cpe:/a:apache:tomcat:-", dependency.getVulnerabilities().getVulnerabilities().get(0).getVulnerableSoftware().get(0));
            } else if (dependency.getFileName().equals("ant-testutil.jar")) {
                foundCount++;
                Assert.assertNull(dependency.getVulnerabilities().getVulnerabilities());
                Assert.assertEquals(1, dependency.getVulnerabilities().getSuppressedVulnerabilities().size());
            }
        }
        Assert.assertEquals(2, foundCount);
    }
}
