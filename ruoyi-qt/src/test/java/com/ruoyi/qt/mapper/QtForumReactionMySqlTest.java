package com.ruoyi.qt.mapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.domain.QtForumReaction;
import static org.junit.jupiter.api.Assertions.*;

/** 只连接本机一次性测试库。设置 QUANTA_FORUM_MYSQL_TEST=true 显式执行，不触碰开发/生产库。 */
@EnabledIfEnvironmentVariable(named = "QUANTA_FORUM_MYSQL_TEST", matches = "true")
class QtForumReactionMySqlTest
{
    private static final String URL = "jdbc:mysql://127.0.0.1:33367/forum_reactions_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setup() throws Exception
    {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("sql/2026-10-23-forum-reactions.sql"))) root = root.getParent();
        assertNotNull(root);
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("create table if not exists sys_user(user_id bigint primary key, real_name varchar(64), nick_name varchar(64), member_department varchar(20), avatar varchar(200), is_quanta_member char(1) default '1', status char(1) default '0', del_flag char(1) default '0')");
            c.createStatement().execute("drop table if exists sys_role");
            c.createStatement().execute("create table sys_role(role_id bigint primary key,role_key varchar(50),status char(1) default '0',del_flag char(1) default '0')");
            c.createStatement().execute("create table if not exists sys_user_role(user_id bigint,role_id bigint,primary key(user_id,role_id))");
            c.createStatement().execute("create table if not exists qt_member_profile(user_id bigint primary key, community_nickname varchar(64))");
            c.createStatement().execute("drop table if exists qt_forum_comment");
            c.createStatement().execute("drop table if exists qt_forum_post");
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-14-qt-forum.sql")));
            c.createStatement().execute("alter table qt_forum_post add view_count bigint not null default 0");
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-23-forum-reactions.sql")));
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-24-forum-visibility.sql")));
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-24-forum-visibility.sql")));
            ScriptUtils.executeSqlScript(c, new FileSystemResource(root.resolve("sql/2026-10-23-forum-reactions.sql")));
        }
        Configuration config = new Configuration(new Environment("forum-test", new JdbcTransactionFactory(),
                new UnpooledDataSource("com.mysql.cj.jdbc.Driver", URL, "root", "")));
        config.getTypeAliasRegistry().registerAlias("QtForumPost", QtForumPost.class);
        config.addInterceptor(new PageInterceptor());
        for (String name : List.of("QtForumPostMapper", "QtForumReactionMapper", "QtForumImageMapper"))
        {
            String resource = "mapper/" + name + ".xml";
            try (InputStream stream = QtForumReactionMySqlTest.class.getClassLoader().getResourceAsStream(resource))
            {
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    @BeforeEach
    void reset() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("delete from qt_forum_post_like");
            c.createStatement().execute("delete from qt_forum_post_favorite");
            c.createStatement().execute("delete from qt_forum_post");
            c.createStatement().execute("delete from sys_user_role");
            c.createStatement().execute("delete from sys_role");
            c.createStatement().execute("delete from sys_user");
            c.createStatement().execute("delete from qt_forum_image");
            c.createStatement().execute("insert into sys_user(user_id) values(1),(7),(8),(9),(10)");
            c.createStatement().execute("insert into qt_forum_post(post_id,title,category,content,author_id) values(1,'first','TECH','body',8),(2,'second','PROJECT','body',9)");
        }
    }

    @Test
    void sqlIsIdempotentPrivateAndReturnsMappedCounts()
    {
        try (SqlSession session = factory.openSession(true))
        {
            QtForumReactionMapper r = session.getMapper(QtForumReactionMapper.class);
            QtForumPostMapper p = session.getMapper(QtForumPostMapper.class);
            assertTrue(r.isSchemaReady());
            r.insertLike(1L, 8L); r.insertLike(1L, 8L); r.insertLike(1L, 9L);
            r.insertFavorite(1L, 8L); r.insertFavorite(2L, 8L);
            QtForumReaction state = r.selectStates(List.of(1L, 2L), 8L).stream().filter(item -> item.getPostId() == 1L).findFirst().orElseThrow();
            assertEquals(1L, state.getPostId());
            assertEquals(2, state.getLikeCount()); assertTrue(state.isLiked()); assertTrue(state.isFavorited());
            assertEquals(0, p.selectPersonalPostList(null, 10L, "liked").size());
            assertEquals(List.of(2L, 1L), p.selectPersonalPostList(null, 8L, "favorites").stream().map(QtForumPost::getPostId).toList());
            assertEquals(1, p.selectPersonalPostList("TECH", 8L, "favorites").size());
            PageHelper.startPage(2, 1);
            List<QtForumPost> page = p.selectPersonalPostList(null, 8L, "favorites");
            assertEquals(2, ((Page<?>) page).getTotal());
            assertEquals(List.of(1L), page.stream().map(QtForumPost::getPostId).toList());
            r.deleteLike(1L, 8L); r.deleteLike(1L, 8L);
            assertEquals(1, r.selectStates(List.of(1L), 8L).get(0).getLikeCount());
            assertFalse(r.selectStates(List.of(1L), 8L).get(0).isLiked());
        }
    }

    @Test
    void deletedPostAndConcurrentWriteUseTheSameLock() throws Exception
    {
        CountDownLatch locked = new CountDownLatch(1), proceed = new CountDownLatch(1), writerStarted = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try
        {
            var deleting = pool.submit(() -> {
                try (SqlSession s = factory.openSession(false))
                {
                    var p = s.getMapper(QtForumPostMapper.class); var r = s.getMapper(QtForumReactionMapper.class);
                    assertNotNull(p.lockPostById(1L)); locked.countDown();
                    assertTrue(proceed.await(5, TimeUnit.SECONDS));
                    r.deleteLikesByPostId(1L); r.deleteFavoritesByPostId(1L); p.deletePostById(1L); s.commit();
                }
                return true;
            });
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var adding = pool.submit(() -> {
                try (SqlSession s = factory.openSession(false))
                {
                    // Service only inserts if the locked post still exists.
                    var p = s.getMapper(QtForumPostMapper.class);
                    writerStarted.countDown();
                    if (p.lockPostById(1L) != null) s.getMapper(QtForumReactionMapper.class).insertLike(1L, 8L);
                    s.commit();
                }
                return true;
            });
            assertTrue(writerStarted.await(5, TimeUnit.SECONDS));
            proceed.countDown(); assertTrue(deleting.get(8, TimeUnit.SECONDS)); assertTrue(adding.get(8, TimeUnit.SECONDS));
            try (Connection c = DriverManager.getConnection(URL, "root", ""); var rs = c.createStatement().executeQuery("select count(*) from qt_forum_post_like where post_id=1"))
            { assertTrue(rs.next()); assertEquals(0, rs.getInt(1)); }
        }
        finally { proceed.countDown(); pool.shutdownNow(); }
    }

    @Test
    void concurrentRepeatedLikesProduceOneRelation() throws Exception
    {
        var pool = Executors.newFixedThreadPool(4);
        try
        {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 8; i++) jobs.add(pool.submit(() -> {
                try (SqlSession s = factory.openSession(false))
                {
                    assertNotNull(s.getMapper(QtForumPostMapper.class).lockPostById(1L));
                    s.getMapper(QtForumReactionMapper.class).insertLike(1L, 8L); s.commit();
                }
                return true;
            }));
            for (var job : jobs) assertTrue(job.get(8, TimeUnit.SECONDS));
            try (SqlSession s = factory.openSession(true))
            { assertEquals(1, s.getMapper(QtForumReactionMapper.class).selectStates(List.of(1L), 8L).get(0).getLikeCount()); }
        }
        finally { pool.shutdownNow(); }
    }

    @Test
    void visibilityFiltersBeforePaginationAndTracksCurrentDatabaseRoles() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("update qt_forum_post set visibility='MANAGERS' where post_id=2");
            c.createStatement().execute("insert into sys_role(role_id,role_key) values(1,'qt_manager'),(2,'qt_mgmt'),(3,'ceo'),(4,'admin')");
            c.createStatement().execute("insert into sys_user_role values(7,1),(8,4)");
        }
        try (SqlSession s = factory.openSession(true))
        {
            QtForumPostMapper p = s.getMapper(QtForumPostMapper.class);
            assertTrue(p.canManagePosts(1L)); assertTrue(p.canManagePosts(7L)); assertFalse(p.canManagePosts(8L));
            QtForumPost query = new QtForumPost(); query.setViewerId(8L);
            PageHelper.startPage(1, 1);
            var rows = p.selectPostList(query);
            assertEquals(1, ((Page<?>) rows).getTotal()); assertEquals(1L, rows.get(0).getPostId());
            query.setViewerId(7L);
            assertEquals(2, p.selectPostList(query).size());
            s.getMapper(QtForumReactionMapper.class).insertFavorite(2L, 8L);
            assertTrue(p.selectPersonalPostList(null, 8L, "favorites").isEmpty());
            query.setAuthorId(9L); query.setViewerId(8L);
            assertTrue(p.selectPostList(query).isEmpty());
        }
        for (String key : List.of("qt_mgmt", "ceo"))
        {
            try (Connection c = DriverManager.getConnection(URL, "root", ""))
            { c.createStatement().execute("update sys_role set role_key='" + key + "' where role_id=1"); }
            try (SqlSession s = factory.openSession(true))
            { assertTrue(s.getMapper(QtForumPostMapper.class).canManagePosts(7L)); }
        }
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        { c.createStatement().execute("update sys_role set status='1' where role_id=1"); }
        try (SqlSession s = factory.openSession(true))
        { assertFalse(s.getMapper(QtForumPostMapper.class).canManagePosts(7L)); }
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        { c.createStatement().execute("update sys_role set status='0',del_flag='2' where role_id=1"); }
        try (SqlSession s = factory.openSession(true))
        { assertFalse(s.getMapper(QtForumPostMapper.class).canManagePosts(7L)); }
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        { c.createStatement().execute("update sys_role set del_flag='0' where role_id=1"); }
        for (String change : List.of("is_quanta_member='0'", "is_quanta_member='1',status='1'", "status='0',del_flag='2'"))
        {
            try (Connection c = DriverManager.getConnection(URL, "root", ""))
            { c.createStatement().execute("update sys_user set " + change + " where user_id=7"); }
            try (SqlSession s = factory.openSession(true))
            { assertFalse(s.getMapper(QtForumPostMapper.class).canManagePosts(7L)); }
        }
    }

    @Test
    void imageMetadataAndBindingRoundTrip()
    {
        try (SqlSession s = factory.openSession(true))
        {
            QtForumImageMapper mapper = s.getMapper(QtForumImageMapper.class);
            var image = new com.ruoyi.qt.domain.QtForumImage();
            image.setImageId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); image.setOwnerId(8L);
            image.setStoragePath("upload/qt/forum-private/a.png"); image.setMediaType("image/png");
            assertEquals(1, mapper.insertImage(image));
            assertNull(mapper.lockImage(image.getImageId()).getPostId());
            assertEquals(1, mapper.bindImage(image.getImageId(), 1L));
            assertEquals(1L, mapper.selectImage(image.getImageId()).getPostId());
            assertEquals("image/png", mapper.selectImage(image.getImageId()).getMediaType());
        }
    }
}
