package com.example.materialstudio.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 集成测试的常用请求封装。 */
public final class Api {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Api() {
    }

    public static String session(MockMvc mvc) throws Exception {
        MvcResult result = mvc.perform(get("/api/session")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("MS_SESSION");
        if (cookie == null) {
            throw new IllegalStateException("会话接口未下发 Cookie");
        }
        return cookie.getValue();
    }

    public static JsonNode upload(MockMvc mvc, String session, String templateId, byte[] bytes,
                                  String filename, String mimeType) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/uploads")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", filename, mimeType, bytes))
                        .param("templateId", templateId)
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    public static String createTask(MockMvc mvc, String session, String templateId,
                                    java.util.List<String> assetIds) throws Exception {
        MvcResult result = mvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(JSON.writeValueAsString(java.util.Map.of("templateId", templateId,
                                "assetIds", assetIds)))
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).path("taskId").asText();
    }

    public static JsonNode taskDetail(MockMvc mvc, String session, String taskId) throws Exception {
        MvcResult result = mvc.perform(get("/api/tasks/" + taskId)
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    public static JsonNode taskList(MockMvc mvc, String session) throws Exception {
        MvcResult result = mvc.perform(get("/api/tasks/mine")
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    public static ObjectMapper json() {
        return JSON;
    }
}
