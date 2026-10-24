package com.ruoyi.qt.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.domain.QtPageSiteSaveResult;
import com.ruoyi.qt.service.IQtPageDeployService;
import com.ruoyi.qt.service.IQtPageSiteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QtPageSiteControllerTest
{
    @Mock
    private IQtPageSiteService siteService;

    @Mock
    private IQtPageDeployService deployService;

    @InjectMocks
    private QtPageSiteController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new QtPagesExceptionHandler())
                .build();
    }

    @Test
    void createReturnsSiteAndDeployment() throws Exception
    {
        QtPageSite site = new QtPageSite();
        site.setSiteId(9L);
        site.setSlug("octo-lab");
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(20L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(siteService.create(any())).thenReturn(new QtPageSiteSaveResult(site, deployment));

        mockMvc.perform(post("/qt/pages/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"https://github.com/octo/hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.site.slug").value("octo-lab"))
                .andExpect(jsonPath("$.data.deployment.status").value("PENDING"));
    }

    @Test
    void validationFailureMapsToRealHttp400() throws Exception
    {
        when(siteService.create(any()))
                .thenThrow(new ServiceException("仓库地址不能为空", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/qt/pages/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void duplicateKeyMapsToRealHttp409() throws Exception
    {
        when(siteService.create(any())).thenThrow(new DuplicateKeyException("uk_qt_page_slug"));

        mockMvc.perform(post("/qt/pages/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"https://github.com/octo/hello\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void mySiteReturnsNullDataWhenNotCreated() throws Exception
    {
        when(siteService.getOwnSite()).thenReturn(null);

        mockMvc.perform(get("/qt/pages/sites/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void foreignSiteUpdateMapsToRealHttp404() throws Exception
    {
        when(siteService.update(eq(99L), any()))
                .thenThrow(new ServiceException("站点不存在", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/qt/pages/sites/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"https://github.com/octo/hello\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void manualDeployReturnsQueuedDeployment() throws Exception
    {
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setDeploymentId(30L);
        deployment.setRequestVersion(2L);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        when(deployService.requestDeploy(eq(9L), eq(QtPageDeployment.TRIGGER_MANUAL))).thenReturn(deployment);

        mockMvc.perform(post("/qt/pages/sites/9/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestVersion").value(2))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void unexpectedErrorMapsToRealHttp500() throws Exception
    {
        when(siteService.getOwnSite()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/qt/pages/sites/me"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
