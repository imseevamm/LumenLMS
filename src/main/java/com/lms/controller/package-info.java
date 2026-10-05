/**
 * Intentionally minimal: in this project, "controller" responsibilities are split between
 * {@link com.lms.service} (business logic, validation, orchestration — the part of a controller
 * that is UI-framework-agnostic) and {@link com.lms.ui} (JavaFX event wiring and navigation — the
 * part of a controller that is tightly coupled to the presentation framework). Screens are built
 * programmatically rather than via FXML, so there are no FXML controller classes to bind here;
 * see README.md ("Technology Stack" note) for the full rationale.
 */
package com.lms.controller;
