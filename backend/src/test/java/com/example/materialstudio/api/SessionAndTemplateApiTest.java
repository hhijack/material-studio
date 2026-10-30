package com.example.materialstudio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.materialstudio.support.Api;
import com.example.materialstudio.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 会话与模板接口（R-02、R-10 的服务端行为）。 */
class SessionAndTemplateApiTest extends BaseIntegrationTest {

    @Test
    @DisplayName("首次访问建立匿名会话并下发 Cookie；携带 Cookie 复用同一用户")
    void sessionCreateAndReuse() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/session")).andExpect(status().isOk()).andReturn();
        Cookie cookie = first.getResponse().getCookie("MS_SESSION");
        assertThat(cookie).isNotNull();

        // 带 Cookie 再次访问：复用同一用户，不新建
        MvcResult second = mockMvc.perform(get("/api/session").cookie(cookie)).andExpect(status().isOk()).andReturn();
        assertThat(second.getResponse().getContentAsString()).contains(cookie.getValue());

        // 不带 Cookie 的全新访问会得到新身份（用于演示隔离）
        MvcResult fresh = mockMvc.perform(get("/api/session")).andExpect(status().isOk()).andReturn();
        assertThat(fresh.getResponse().getCookie("MS_SESSION").getValue()).isNotEqualTo(cookie.getValue());
    }

    @Test
    @DisplayName("修改昵称后查询会话返回新昵称")
    void updateNickname() throws Exception {
        String session = Api.session(mockMvc);
        mockMvc.perform(patch("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"星尘同学\"}")
                        .cookie(new Cookie("MS_SESSION", session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("星尘同学"));
    }

    @Test
    @DisplayName("模板列表返回种子数据，分类筛选字段齐全")
    void templateList() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].title").isNotEmpty())
                .andExpect(jsonPath("$[0].category").isNotEmpty())
                .andExpect(jsonPath("$[0].requiredImageCount").isNumber())
                .andExpect(jsonPath("$[0].acceptedMimeTypes").isArray());
    }

    @Test
    @DisplayName("分类接口返回去重后的分类与数量")
    void categories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("模板详情存在与不存在的路径")
    void templateDetail() throws Exception {
        mockMvc.perform(get("/api/templates/travel-postcard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("旅行明信片"));

        mockMvc.perform(get("/api/templates/not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEMPLATE_NOT_FOUND"));
    }
}
