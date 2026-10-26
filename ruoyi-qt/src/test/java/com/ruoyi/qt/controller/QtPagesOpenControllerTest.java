package com.ruoyi.qt.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.service.IQtPagesOpenService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QtPagesOpenControllerTest
{
    @Mock
    private IQtPagesOpenService openService;

    @InjectMocks
    private QtPagesOpenController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new QtPagesOpenExceptionHandler())
                .build();
    }

    @Test
    void listPostsReturnsItemsAndTotal() throws Exception
    {
        when(openService.listPosts(eq("demo-lab"), eq(2), eq(10))).thenReturn(new IQtPagesOpenService.PageResult<>(
                java.util.List.of(new IQtPagesOpenService.PublicPostSummary(1L, "标题", "TECH", "摘要", 2L, "2026-10-25 10:00:00")),
                1L, 2, 10));

        mockMvc.perform(get("/open/pages/demo-lab/posts").param("pageNum", "2").param("pageSize", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("标题"));
    }

    @Test
    void postDetailReturnsMarkdown() throws Exception
    {
        when(openService.getPost("demo-lab", 7L)).thenReturn(new IQtPagesOpenService.PublicPostDetail(
                7L, "文章", "LIFE", "# 正文", 0L, "2026-10-25 10:00:00", "橘猫"));

        mockMvc.perform(get("/open/pages/demo-lab/posts/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("# 正文"))
                .andExpect(jsonPath("$.data.authorNickname").value("橘猫"));
    }

    @Test
    void commentsReturnNicknamesOnly() throws Exception
    {
        when(openService.listComments(eq("demo-lab"), eq(7L), eq(null), eq(null)))
                .thenReturn(new IQtPagesOpenService.PageResult<>(
                        java.util.List.of(new IQtPagesOpenService.PublicComment(11L, null, "留言", "小鹿", "", "2026-10-25 10:00:00")),
                        1L, 1, 20));

        mockMvc.perform(get("/open/pages/demo-lab/posts/7/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].authorNickname").value("小鹿"))
                .andExpect(jsonPath("$.data.items[0].authorName").doesNotExist());
    }

    @Test
    void missingSiteOrPostMapsToRealHttp404() throws Exception
    {
        when(openService.listPosts(eq("ghost"), eq(null), eq(null)))
                .thenThrow(new ServiceException("站点不存在", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/open/pages/ghost/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void invalidQueryParameterMapsToRealHttp400() throws Exception
    {
        mockMvc.perform(get("/open/pages/demo-lab/posts").param("pageNum", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rateLimitMapsToRealHttp429() throws Exception
    {
        when(openService.listPosts(eq("demo-lab"), eq(null), eq(null)))
                .thenThrow(new ServiceException("访问过于频繁，请稍候再试", HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(get("/open/pages/demo-lab/posts"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void unexpectedErrorMapsToRealHttp500() throws Exception
    {
        when(openService.listPosts(eq("demo-lab"), eq(null), eq(null)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/open/pages/demo-lab/posts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
