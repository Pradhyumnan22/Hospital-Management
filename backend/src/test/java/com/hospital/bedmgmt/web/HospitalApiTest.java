package com.hospital.bedmgmt.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginRequiresCsrfAndRolesAreEnforced() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminDemo#2026\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics/overview")).andExpect(status().isUnauthorized());

        MockHttpSession admin = login("admin", "AdminDemo#2026");
        mockMvc.perform(get("/api/auth/me").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        MockHttpSession clinical = login("clinical", "ClinicDemo#2026");
        mockMvc.perform(post("/api/patients")
                        .session(clinical)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody("MRN-CLINICAL")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/staff").session(clinical))
                .andExpect(status().isForbidden());

        MockHttpSession admission = login("admission", "AdmitDemo#2026");
        mockMvc.perform(post("/api/patients")
                        .session(admission)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody("MRN-2001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicalRecordNumber").value("MRN-2001"));

        mockMvc.perform(post("/api/wards")
                        .session(admission)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ZZZ\",\"name\":\"Blocked\",\"wardType\":\"GENERAL\",\"floorNumber\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admissionRevalidatesRulesAndDischargeFreesTheKey() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo#2026");
        mockMvc.perform(get("/api/analytics/overview").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalBeds", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.measured").value(true));

        mockMvc.perform(get("/api/analytics/forecast").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next24Hours", hasSize(24)))
                .andExpect(jsonPath("$.next7Days", hasSize(7)))
                .andExpect(jsonPath("$.history", hasSize(14)));

        String patientJson = mockMvc.perform(post("/api/patients")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody("MRN-3001")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = objectMapper.readTree(patientJson).get("id").asLong();

        JsonNode beds = objectMapper.readTree(mockMvc.perform(get("/api/beds").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long generalBed = -1;
        for (JsonNode bed : beds) {
            if ("GENERAL".equals(bed.get("wardType").asText())
                    && "AVAILABLE".equals(bed.get("state").asText())
                    && bed.get("oxygen").asBoolean()) {
                generalBed = bed.get("id").asLong();
                break;
            }
        }
        if (generalBed < 0) {
            throw new AssertionError("Expected an available general bed with oxygen");
        }

        String rejected = """
                {"patientId":%d,"bedId":%d,"diagnosis":"Needs ICU","requiredWardType":"ICU","preferredWardType":"ICU","isolationRequired":false,"ventilatorRequired":true,"monitorRequired":true,"oxygenRequired":true}
                """.formatted(patientId, generalBed);
        mockMvc.perform(post("/api/admissions")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejected))
                .andExpect(status().isConflict());

        String accepted = """
                {"patientId":%d,"bedId":%d,"diagnosis":"Synthetic observation","requiredWardType":"GENERAL","preferredWardType":"GENERAL","isolationRequired":false,"ventilatorRequired":false,"monitorRequired":false,"oxygenRequired":true}
                """.formatted(patientId, generalBed);
        String admissionJson = mockMvc.perform(post("/api/admissions")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accepted))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long admissionId = objectMapper.readTree(admissionJson).get("id").asLong();

        mockMvc.perform(post("/api/admissions")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accepted))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admissions/" + admissionId + "/discharge")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Synthetic discharge\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCHARGED"));

        mockMvc.perform(patch("/api/beds/" + generalBed + "/state")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AVAILABLE"));
    }

    @Test
    void deleteIsBlockedWhenHistoryExistsAndAuditIsReadable() throws Exception {
        MockHttpSession admin = login("admin", "AdminDemo#2026");
        String created = mockMvc.perform(post("/api/patients")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody("MRN-DELETE-1")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(delete("/api/patients/" + patientId).session(admin).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/audit").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'DELETE_PATIENT')]").exists());

        JsonNode patients = objectMapper.readTree(mockMvc.perform(get("/api/patients").session(admin))
                .andReturn().getResponse().getContentAsString());
        long seededId = patients.get(0).get("id").asLong();
        mockMvc.perform(delete("/api/patients/" + seededId).session(admin).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/audit").session(login("clinical", "ClinicDemo#2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void recommendationExplainsEligibleBeds() throws Exception {
        MockHttpSession clinical = login("clinical", "ClinicDemo#2026");
        JsonNode patients = objectMapper.readTree(mockMvc.perform(get("/api/patients").session(clinical))
                .andReturn().getResponse().getContentAsString());
        long patientId = patients.get(0).get("id").asLong();
        String body = """
                {"patientId":%d,"diagnosis":"Synthetic review","requiredWardType":"ICU","preferredWardType":"ICU","isolationRequired":false,"ventilatorRequired":true,"monitorRequired":true,"oxygenRequired":true}
                """.formatted(patientId);
        mockMvc.perform(post("/api/recommendations")
                        .session(clinical)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.candidates[0].wardType").value("ICU"))
                .andExpect(jsonPath("$.candidates[0].explanations").isArray());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String patientBody(String mrn) {
        return """
                {"medicalRecordNumber":"%s","firstName":"Test","lastName":"Patient","dateOfBirth":"%s","sex":"UNKNOWN","phone":"5550199"}
                """.formatted(mrn, LocalDate.now().minusYears(30));
    }
}
