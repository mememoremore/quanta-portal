package com.ruoyi.qt.mapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 只连接本机一次性测试库。设置 QUANTA_PAGES_MYSQL_TEST=true 显式执行，不触碰开发/生产库。 */
@EnabledIfEnvironmentVariable(named = "QUANTA_PAGES_MYSQL_TEST", matches = "true")
class QtPagesMySqlTest
{
    private static final String URL = "jdbc:mysql://127.0.0.1:33367/qt_pages_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setup() throws Exception
    {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("sql/2026-10-25-qt-pages.sql")))
        {
            root = root.getParent();
        }
        assertNotNull(root);
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("create table if not exists sys_user(user_id bigint primary key, real_name varchar(64), nick_name varchar(64), member_department varchar(20), avatar varchar(200), is_quanta_member char(1) default '1', status char(1) default '0', del_flag char(1) default '0')");
            c.createStatement().execute("create table if not exists qt_member_profile(user_id bigint primary key, community_nickname varchar(64))");
            // 幂等：同一脚本连续执行两次不破坏数据
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-25-qt-pages.sql")));
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-25-qt-pages.sql")));
        }
        Configuration config = new Configuration(new Environment("pages-test", new JdbcTransactionFactory(),
                new UnpooledDataSource("com.mysql.cj.jdbc.Driver", URL, "root", "")));
        config.getTypeAliasRegistry().registerAlias("QtPageSite", QtPageSite.class);
        config.getTypeAliasRegistry().registerAlias("QtPageDeployment", QtPageDeployment.class);
        for (String name : List.of("QtPageSiteMapper", "QtPageDeploymentMapper"))
        {
            String resource = "mapper/" + name + ".xml";
            try (InputStream stream = QtPagesMySqlTest.class.getClassLoader().getResourceAsStream(resource))
            {
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    private static void reset() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("delete from qt_page_deployment");
            c.createStatement().execute("delete from qt_page_site");
        }
    }

    @Test
    void siteAndDeploymentRoundTrip() throws Exception
    {
        reset();
        try (SqlSession session = factory.openSession(true))
        {
            QtPageSiteMapper siteMapper = session.getMapper(QtPageSiteMapper.class);
            QtPageDeploymentMapper deploymentMapper = session.getMapper(QtPageDeploymentMapper.class);

            QtPageSite site = newSite(7L, "alice-lab");
            siteMapper.insertSite(site);
            assertNotNull(site.getSiteId());

            QtPageSite loaded = siteMapper.selectById(site.getSiteId());
            assertEquals("alice-lab", loaded.getSlug());
            assertEquals("pages", loaded.getBranch());
            assertEquals("/", loaded.getPublishDir());
            assertEquals("ENABLED", loaded.getStatus());
            assertEquals(siteMapper.selectByUserId(7L).getSiteId(), loaded.getSiteId());
            assertEquals(siteMapper.selectBySlug("alice-lab").getSiteId(), loaded.getSiteId());

            QtPageDeployment deployment = newDeployment(loaded.getSiteId(), 1L);
            deploymentMapper.insertDeployment(deployment);
            assertNotNull(deployment.getDeploymentId());
            assertEquals(deployment.getDeploymentId(),
                    deploymentMapper.selectBySiteAndRequestVersion(loaded.getSiteId(), 1L).getDeploymentId());

            deployment.setStatus(QtPageDeployment.STATUS_SUCCESS);
            deployment.setReleaseAvailable(true);
            deploymentMapper.updateDeployment(deployment);
            assertEquals(QtPageDeployment.STATUS_SUCCESS,
                    deploymentMapper.selectById(deployment.getDeploymentId()).getStatus());

            assertEquals(1, deploymentMapper.markSupersededBefore(loaded.getSiteId(), 99L));
            assertEquals(QtPageDeployment.STATUS_SUPERSEDED,
                    deploymentMapper.selectById(deployment.getDeploymentId()).getStatus());
        }
    }

    @Test
    void uniqueUserAndSlugConstraintsRejectDuplicates() throws Exception
    {
        reset();
        try (SqlSession session = factory.openSession(true))
        {
            QtPageSiteMapper mapper = session.getMapper(QtPageSiteMapper.class);
            mapper.insertSite(newSite(7L, "dup-slug"));
            assertThrows(Exception.class, () -> mapper.insertSite(newSite(7L, "other-slug")));
            assertThrows(Exception.class, () -> mapper.insertSite(newSite(8L, "dup-slug")));
        }
    }

    @Test
    void concurrentSameSlugCreateProducesOneWinner() throws Exception
    {
        reset();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try
        {
            Callable<Boolean> first = () -> {
                try (SqlSession session = factory.openSession(true))
                {
                    session.getMapper(QtPageSiteMapper.class).insertSite(newSite(7L, "race-slug"));
                    return true;
                }
                catch (Exception e)
                {
                    return false;
                }
            };
            Callable<Boolean> second = () -> {
                try (SqlSession session = factory.openSession(true))
                {
                    session.getMapper(QtPageSiteMapper.class).insertSite(newSite(8L, "race-slug"));
                    return true;
                }
                catch (Exception e)
                {
                    return false;
                }
            };
            Future<Boolean> a = pool.submit(first);
            Future<Boolean> b = pool.submit(second);
            assertTrue(a.get(10, TimeUnit.SECONDS) ^ b.get(10, TimeUnit.SECONDS));
            try (SqlSession session = factory.openSession(true))
            {
                assertEquals(1, session.getMapper(QtPageSiteMapper.class).selectBySlug("race-slug") != null ? 1 : 0);
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    @Test
    void rowLockSerializesRequestVersionAdvance() throws Exception
    {
        reset();
        try (SqlSession session = factory.openSession(true))
        {
            QtPageSiteMapper mapper = session.getMapper(QtPageSiteMapper.class);
            mapper.insertSite(newSite(7L, "lock-slug"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try
        {
            Future<?> a = pool.submit(() -> advanceTwice());
            Future<?> b = pool.submit(() -> advanceTwice());
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
            try (SqlSession session = factory.openSession(true))
            {
                assertEquals(4L, session.getMapper(QtPageSiteMapper.class).selectBySlug("lock-slug").getRequestVersion());
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    private static void advanceTwice()
    {
        for (int i = 0; i < 2; i++)
        {
            try (SqlSession session = factory.openSession(false))
            {
                QtPageSiteMapper mapper = session.getMapper(QtPageSiteMapper.class);
                QtPageSite locked = mapper.selectBySlug("lock-slug");
                QtPageSite row = mapper.selectForUpdate(locked.getSiteId());
                row.setRequestVersion(row.getRequestVersion() + 1);
                mapper.updateSite(row);
                session.commit();
            }
        }
    }

    private static QtPageSite newSite(Long userId, String slug)
    {
        QtPageSite site = new QtPageSite();
        site.setUserId(userId);
        site.setSlug(slug);
        site.setRepoUrl("https://github.com/octo/hello");
        site.setRepoOwner("octo");
        site.setRepoName("hello");
        site.setBranch("pages");
        site.setPublishDir("/");
        site.setConfigVersion(1L);
        site.setRequestVersion(0L);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        return site;
    }

    private static QtPageDeployment newDeployment(Long siteId, Long requestVersion)
    {
        QtPageDeployment deployment = new QtPageDeployment();
        deployment.setSiteId(siteId);
        deployment.setRequestVersion(requestVersion);
        deployment.setConfigVersion(1L);
        deployment.setRepoOwner("octo");
        deployment.setRepoName("hello");
        deployment.setBranch("pages");
        deployment.setPublishDir("/");
        deployment.setTriggerType(QtPageDeployment.TRIGGER_CREATE);
        deployment.setStatus(QtPageDeployment.STATUS_PENDING);
        deployment.setAttemptCount(0);
        deployment.setReleaseAvailable(false);
        return deployment;
    }
}
