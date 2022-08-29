/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package org.dependencytrack.tasks.metrics;

import org.dependencytrack.event.ComponentMetricsUpdateEvent;
import org.dependencytrack.model.AnalysisState;
import org.dependencytrack.model.Component;
import org.dependencytrack.model.DependencyMetrics;
import org.dependencytrack.model.Policy;
import org.dependencytrack.model.PolicyViolation;
import org.dependencytrack.model.Project;
import org.dependencytrack.model.Severity;
import org.dependencytrack.model.ViolationAnalysisState;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.tasks.scanners.AnalyzerIdentity;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentMetricsUpdateTaskTest extends AbstractMetricsUpdateTaskTest {

    @Test
    public void testUpdateComponentMetricsEmpty() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        component = qm.createComponent(component, false);

        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(qm.getPersistenceManager().detachCopy(component)));

        final DependencyMetrics metrics = qm.getMostRecentDependencyMetrics(component);
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isZero();
        assertThat(metrics.getMedium()).isZero();
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isZero();
        assertThat(metrics.getSuppressed()).isZero();
        assertThat(metrics.getFindingsTotal()).isZero();
        assertThat(metrics.getFindingsAudited()).isZero();
        assertThat(metrics.getFindingsUnaudited()).isZero();
        assertThat(metrics.getInheritedRiskScore()).isZero();
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isZero();
    }

    @Test
    public void testUpdateComponentMetricsUnchanged() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        component = qm.createComponent(component, false);

        // Record initial project metrics
        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(qm.getPersistenceManager().detachCopy(component)));
        final DependencyMetrics metrics = qm.getMostRecentDependencyMetrics(component);
        assertThat(metrics.getLastOccurrence()).isEqualTo(metrics.getFirstOccurrence());

        // Run the task a second time, without any metric being changed
        final var beforeSecondRun = new Date();
        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(qm.getPersistenceManager().detachCopy(component)));

        // Ensure that the lastOccurrence timestamp was correctly updated
        qm.getPersistenceManager().refresh(metrics);
        assertThat(metrics.getLastOccurrence()).isNotEqualTo(metrics.getFirstOccurrence());
        assertThat(metrics.getLastOccurrence()).isAfterOrEqualTo(beforeSecondRun);
    }

    @Test
    public void testUpdateComponentMetricsVulnerabilities() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        component = qm.createComponent(component, false);

        // Create an unaudited vulnerability.
        var vulnUnaudited = new Vulnerability();
        vulnUnaudited.setVulnId("INTERNAL-001");
        vulnUnaudited.setSource(Vulnerability.Source.INTERNAL);
        vulnUnaudited.setSeverity(Severity.HIGH);
        vulnUnaudited = qm.createVulnerability(vulnUnaudited, false);
        qm.addVulnerability(vulnUnaudited, component, AnalyzerIdentity.NONE);

        // Create an audited vulnerability.
        var vulnAudited = new Vulnerability();
        vulnAudited.setVulnId("INTERNAL-002");
        vulnAudited.setSource(Vulnerability.Source.INTERNAL);
        vulnAudited.setSeverity(Severity.MEDIUM);
        vulnAudited = qm.createVulnerability(vulnAudited, false);
        qm.addVulnerability(vulnAudited, component, AnalyzerIdentity.NONE);
        qm.makeAnalysis(component, vulnAudited, AnalysisState.NOT_AFFECTED, null, null, null, false);

        // Create a suppressed vulnerability.
        var vulnSuppressed = new Vulnerability();
        vulnSuppressed.setVulnId("INTERNAL-003");
        vulnSuppressed.setSource(Vulnerability.Source.INTERNAL);
        vulnSuppressed.setSeverity(Severity.MEDIUM);
        vulnSuppressed = qm.createVulnerability(vulnSuppressed, false);
        qm.addVulnerability(vulnSuppressed, component, AnalyzerIdentity.NONE);
        qm.makeAnalysis(component, vulnSuppressed, AnalysisState.FALSE_POSITIVE, null, null, null, true);

        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(qm.getPersistenceManager().detachCopy(component)));

        final DependencyMetrics metrics = qm.getMostRecentDependencyMetrics(component);
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isEqualTo(1);
        assertThat(metrics.getMedium()).isEqualTo(1); // One is suppressed
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isEqualTo(2); // One is suppressed
        assertThat(metrics.getSuppressed()).isEqualTo(1);
        assertThat(metrics.getFindingsTotal()).isEqualTo(2); // One is suppressed
        assertThat(metrics.getFindingsAudited()).isEqualTo(1);
        assertThat(metrics.getFindingsUnaudited()).isEqualTo(1);
        assertThat(metrics.getInheritedRiskScore()).isEqualTo(8.0);
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isEqualTo(8.0);
    }

    @Test
    public void testUpdateComponentMetricsPolicyViolations() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        component = qm.createComponent(component, false);

        // Create an unaudited violation.
        createPolicyViolation(component, Policy.ViolationState.FAIL, PolicyViolation.Type.LICENSE);

        // Create an audited violation.
        final PolicyViolation auditedViolation = createPolicyViolation(component, Policy.ViolationState.WARN, PolicyViolation.Type.OPERATIONAL);
        qm.makeViolationAnalysis(component, auditedViolation, ViolationAnalysisState.APPROVED, false);

        // Create a suppressed violation.
        final PolicyViolation suppressedViolation = createPolicyViolation(component, Policy.ViolationState.INFO, PolicyViolation.Type.SECURITY);
        qm.makeViolationAnalysis(component, suppressedViolation, ViolationAnalysisState.REJECTED, true);

        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(qm.getPersistenceManager().detachCopy(component)));

        final DependencyMetrics metrics = qm.getMostRecentDependencyMetrics(component);
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isZero();
        assertThat(metrics.getMedium()).isZero();
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isZero();
        assertThat(metrics.getSuppressed()).isZero();
        assertThat(metrics.getFindingsTotal()).isZero();
        assertThat(metrics.getFindingsAudited()).isZero();
        assertThat(metrics.getFindingsUnaudited()).isZero();
        assertThat(metrics.getInheritedRiskScore()).isZero();
        assertThat(metrics.getPolicyViolationsFail()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsWarn()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsInfo()).isZero(); // Suppressed
        assertThat(metrics.getPolicyViolationsTotal()).isEqualTo(2);
        assertThat(metrics.getPolicyViolationsAudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsUnaudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero(); // Suppressed
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isZero();
    }

}